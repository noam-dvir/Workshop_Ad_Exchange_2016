package modules;

import tau.tac.adx.report.adn.MarketSegment;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import modules.CampaignData;

public class CampaignBidder {
    
    final static double qsRedZone = 0.8;
    final static double qsGrayZone = 0.95;
    
    final static double campaignWinCompetiveFactor = 1.2;
    final static double segmentDensityMax = 1.15;
    final static double maxCompetativeBid = 1.5;
    final static double minBidLimit = 300.0;
    static double g_competiveFactor = 0.5;
    
    
    /*
        this API get the current campaign opportunity and all the current active campaigns
        and it return to the caller the bit that should be send to the server
     */
    public static long getCampaignBid(CampaignData campaign, Map<Integer,CampaignData> allActiveCampaign, int day,CampaignData lastCampaign, double qs){
        double score;
        if (lastCampaign == null){
            // first day
            score = setStartingScore(campaign);
        }
        else{
            score = setScore(campaign, lastCampaign, qs, day, allActiveCampaign);
        }
        
        if (qs==score){
            return (long)(Math.floor(qs*campaign.getreachImps()));
        }
        else if ((long)Math.ceil(score*campaign.getreachImps()) < minBidLimit){
            return (long)(Math.floor(qs*campaign.getreachImps()));
        }
        else{
            return (long)(Math.ceil(score*campaign.getreachImps()));
        }
    }
    
    /*
        on the first day we dont have any data to work with- just the current campaig data
        so this data will be the only thing we look at
     
     */
    public static double setStartingScore(CampaignData campaign){
        double score;
        long campaignLength = campaign.getLength()+1;
        int segmentSize = MarketSegment.marketSegmentSize(campaign.gettargetSegment());
        long reachImp = campaign.getreachImps();
        score=reachImp/(campaignLength*segmentSize);
        return score;
    }
    
    
    /*
        this modulte purpuse is to win campaigns.
        the main thing we look at in order to determain how hard is a campaign is how much its segments are busy,
        the other thing we keep track is if we didnt won a campaign for
     */
    private static double setScore(CampaignData campaign, CampaignData lastCampaign, double qs, int day, Map<Integer,CampaignData> activeCampaigns) {
        activeCampaigns.put(campaign.getId(), campaign);
        double segmentRatio;
        
        
        double maxbid = qs;
        double minbid = 0.1/qs;
        
        double competiveFactor = getCompetiveFactor(lastCampaign, day);
        segmentRatio = getSegmentRatio(campaign, activeCampaigns);
        double returnScore = 0;
        
        if (qs<qsRedZone){
            // our quality score is already too low and we probably wont get any campaigns because of it- so hope to win the random campaigns
            returnScore = maxbid;
        }
        else if(qs < qsGrayZone){
            // out quality score is on the eadge so in order to improve it bid the minimun and finish the campaign
            returnScore = minbid;
        }
        else if(segmentRatio > segmentDensityMax){
            // this campaign segments are bvery busy and if er take it each impression will probably be expensive
            returnScore = maxbid;
        }
        else if(competiveFactor*segmentRatio>=maxCompetativeBid*minbid){
            // our bid will not be comperative at all- instead it is better to hope for winning the random with max bid
            returnScore = maxbid;
        }
        else{
            returnScore = Math.max(minbid,Math.min(competiveFactor*segmentRatio,maxbid));
        }
        return returnScore;
    }
    
    
    // this function control on how badly we want to win the next campaign- the g_competiveFactor
    private static double getCompetiveFactor(CampaignData lastcampaginDay, long day) {
        if (lastcampaginDay.IsWin() == true){
            g_competiveFactor = g_competiveFactor * campaignWinCompetiveFactor;
        }
        else{
            // we didnt won so next time bid lower
            g_competiveFactor  = g_competiveFactor / campaignWinCompetiveFactor;
        }
        return g_competiveFactor;
    }
    
    // this function tells us how much the semgnets of a current campaign are busy
    private static double getSegmentRatio(CampaignData campaign, Map<Integer,CampaignData> activeCampaigns){
        Set<MarketSegment> curMarketSegment = campaign.gettargetSegment();
        long startDay = campaign.getdayStart();
        long endDay = campaign.getdayEnd();
        double segmentRatio = 0;
        
        for(Set<MarketSegment> segment: getBasicSegmentsList()){
            if (isSegmentInSet( curMarketSegment, segment )){
                for (long i=startDay; i<=endDay; ++i){
                    int segmentSize = MarketSegment.marketSegmentSize(segment);
                    double impBySegmentInDay = getSegmentRatioByDay(segment, i, activeCampaigns);
                    segmentRatio = segmentRatio + segmentSize * impBySegmentInDay;
                }
            }
        }
        
        double length = (double)campaign.getLength()+1;
        double segmentSize = (double)MarketSegment.marketSegmentSize(curMarketSegment);
        segmentRatio = segmentRatio/(segmentSize*length);
        return segmentRatio;
    }
    
    
    public static double getSegmentRatioByDay(Set<MarketSegment> basicMarketSegment, long day, Map<Integer,CampaignData> activeCampaigns){
        double segmentRatio = 0;
        CampaignData campaign;
        Set<MarketSegment> CampaignSegment;
        long startDay;
        long endDay;
        double size;
        double reach;
        double length;
        
        for (Map.Entry<Integer, CampaignData> entry : activeCampaigns.entrySet()){
            campaign = entry.getValue();
            CampaignSegment = campaign.gettargetSegment();
            startDay = campaign.getdayStart();
            endDay = campaign.getdayEnd();
            if (startDay<=day && day<=endDay){
                if (isSegmentInSet(CampaignSegment, basicMarketSegment)){
                    length = (double)campaign.getLength()+1;
                    reach = (double)campaign.getreachImps();
                    size = (double)campaign.getSegmentSize();
                    segmentRatio = segmentRatio+(reach/(size*length));
                }
            }
        }
        return segmentRatio;
    }
    
    private static List<Set<MarketSegment>> getBasicSegmentsList() {
        List<Set<MarketSegment>> toReturn = new ArrayList<Set<MarketSegment>>();
        
        MarketSegment male = MarketSegment.MALE;
        MarketSegment female = MarketSegment.FEMALE;
        MarketSegment low = MarketSegment.LOW_INCOME;
        MarketSegment high = MarketSegment.HIGH_INCOME;
        MarketSegment young = MarketSegment.YOUNG;
        MarketSegment old = MarketSegment.OLD;
        
        toReturn.add(MarketSegment.compundMarketSegment3(male,   low,  young));
        toReturn.add(MarketSegment.compundMarketSegment3(male,   low,  old));
        toReturn.add(MarketSegment.compundMarketSegment3(male,   high, young));
        toReturn.add(MarketSegment.compundMarketSegment3(male,   high, old));
        toReturn.add(MarketSegment.compundMarketSegment3(female, low,  young));
        toReturn.add(MarketSegment.compundMarketSegment3(female, low,  old));
        toReturn.add(MarketSegment.compundMarketSegment3(female, high, young));
        toReturn.add(MarketSegment.compundMarketSegment3(female, high, old));

        return toReturn;
    }
    
    private static boolean isSegmentInSet(Set<MarketSegment> segments ,Set<MarketSegment> basicSegment) {
        boolean result;
        for (MarketSegment s: segments){
            result = false;
            for (MarketSegment bs: basicSegment){
                if (s==bs){result=true;break;}
            }
            if(result==false){
                return false;
            }
        }
        return true;
    }
}

