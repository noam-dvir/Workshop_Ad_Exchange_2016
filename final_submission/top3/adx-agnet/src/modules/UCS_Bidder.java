package modules;
import java.util.Map;
import java.lang.*;     // for Math.sqrt


public class UCS_Bidder {
    private static double lastTargetedBidLevel;
    private static double curTargetedBidLevel;
    private static double lastResultLevel;
    private static double lastBid;
    private static double bidLevelOffset;
    private static double bidBidOffset;

    
    public static double calcUCSBid(Map<Integer, CampaignData> myActiveCampaigns, long day)
    {
        System.out.println("[calcUCSBid]: lastBid="+lastBid+", lastResultLevel="+lastResultLevel+", lastTargetedBidLevel="+lastTargetedBidLevel);
        if (day == 0){
            // init params
            System.out.println("[calcUCSBid]: day=0, bid="+lastBid);
            lastBid = 0.3;
            lastTargetedBidLevel = 0.729;
        }
        else if ((myActiveCampaigns.size()==0)||(!isActiveCampaigns(myActiveCampaigns,day))){
            // no active campaign
            lastBid = 0.0;
            lastTargetedBidLevel = 0.729;
            System.out.println("[calcUCSBid]: no campaign, bid="+lastBid);
        }
        else{

            bidLevelOffset = calcBidLevelOffset();
            bidBidOffset = calcBidBidOffset();
            
            curTargetedBidLevel = getSmartReachIndex(myActiveCampaigns, day);
            curTargetedBidLevel = setToLevel(curTargetedBidLevel);
            lastTargetedBidLevel = curTargetedBidLevel;
            System.out.println("[calcUCSBid]: lastBid="+lastBid+", curTargetedBidLevel="+curTargetedBidLevel+", bidBidOffset="+bidBidOffset+", bidLevelOffset="+bidLevelOffset);
            if (lastBid == 0){
                lastBid = 0.5;
            }
            lastBid = lastBid * bidBidOffset * bidLevelOffset;
        }
        System.out.println("[calcUCSBid]: final bid="+lastBid);
        return lastBid;
    }

    /* 
        calc the offet with respect to the result of last UCS bid
        in case we got higher level than wanted- bid less
        in case we got lower level than wanted- bid more
    */
    public static double calcBidLevelOffset(){
        double returnVal;
        if (lastTargetedBidLevel < lastResultLevel){
                // last day we pay too much on our bid- so this time decrease it
                System.out.println("[calcBidLevelOffset]: lastTargetedBidLevel < lastResultLevel");
                returnVal = 0.7;
        }
        else if (lastTargetedBidLevel > lastResultLevel){
            // last day we were sort on our bid- so this time increase it
            System.out.println("[calcBidLevelOffset]: lastTargetedBidLevel > lastResultLevel");
            returnVal = 1.3;
        }
        else{
            returnVal = 1;
        }
            return returnVal;
    }

    /* 
        calc the offet with respect to the result of the current wanted level and the last one
        if last day we wanted higher level than now- bid less than we bid yesturday
        if last day we wanted lower level than now- bid more than we bid yesturday
    */
    public static double calcBidBidOffset(){
        double returnVal;
        if (lastTargetedBidLevel < curTargetedBidLevel){
            System.out.println("[calcBidBidOffset]: lastTargetedBidLevel < curTargetedBidLevel");
            returnVal = 1.3;
        }
        else if (lastTargetedBidLevel > curTargetedBidLevel){
            // last day we were sort on our bid- so this time increase it
            System.out.println("[calcBidBidOffset]: lastTargetedBidLevel > curTargetedBidLevel");
            returnVal = 0.7;
        }
        else{
            returnVal = 1;
        }
            return returnVal;
    }
    
    /* 
        normal the result of the wanted UCS level
    */
    public static double setToLevel(double bidLevel){
        if (bidLevel>0.9){
            bidLevel = 0.9;
        }
        else if (bidLevel>0.81){
            bidLevel = 0.81;
        }
        else if (bidLevel>0.729){
            bidLevel = 0.729;
        }
        else if (bidLevel>0.6561){
            bidLevel = 0.6561;
        }
        else if (bidLevel>0.59049){
            bidLevel = 0.59049;
        }
        else if (bidLevel>0.531441){
            bidLevel = 0.531441;
        }
        return bidLevel;
    }
    
    // return a num from 0-1  smartReachIndex
    public static double getSmartReachIndex(Map<Integer, CampaignData> currentCampaigns, long day)
    {
        double smartReachIndex = 0;
        long reach;
        long daysLeft;
        long campaignCounter = 0;
        double totalImpToGo = 0;
        double dayRemain;

        for (CampaignData campaign : currentCampaigns.values())
        {
            campaignCounter++;
            double impToGo = (double)campaign.impsTogo();
            totalImpToGo += impToGo;
            reach= campaign.getreachImps();
            dayRemain = (double)(campaign.getdayEnd()-day + 1);
            double weight = 1.0/dayRemain; // a num from 0-1- close it to 1
            weight = Math.sqrt(Math.sqrt(weight));
            smartReachIndex += impToGo * weight;
            System.out.println("[getSmartReachIndex]: smartReachIndex += impToGo * weight, ("+smartReachIndex+"+="+impToGo+"*"+weight+")");
        }
        smartReachIndex = smartReachIndex / totalImpToGo;
        System.out.println("[getSmartReachIndex]: smartReachIndex="+smartReachIndex+", campaignCounter="+campaignCounter);
        return smartReachIndex;
    }
    
    public static void setLastResultLevel(double _lastResultLevel)
    {
        lastResultLevel = _lastResultLevel;
    }
    public static boolean isActiveCampaigns(Map<Integer, CampaignData> myActiveCampaigns,long day)
    {
        long dayUCSFor = day + 1;
        for (CampaignData campaign : myActiveCampaigns.values())
        {
            if ((dayUCSFor >= campaign.getdayStart())&& (dayUCSFor <= campaign.getdayEnd())&& (campaign.impsTogo() > 0))
            {
                return true;
            }
        }
        return false;
    }
}


