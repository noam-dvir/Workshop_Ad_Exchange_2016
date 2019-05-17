package modules;

import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.demand.CampaignStats;

import java.util.List;
import java.util.Set;

public class CampaignData {
	
		/* campaign attributes as set by server */
		long reachImps;
		long dayStart;
		long dayEnd;
		Set<MarketSegment> targetSegment;
		double videoCoef;
		double mobileCoef;
		int id;
		private AdxQuery[] campaignQueries; //array of queries relvent for the campaign.
		
		/* campaign info as reported */
		CampaignStats stats; 
		double budget;

		double remainingBudget;

		double campaignDifficulty;
		Integer SegmentSize;
		double MyBid;
		Boolean IsWin;
		Double MyRating;
		
		double impressionRatio;
		double currentRatio;
		double ratioTillFinish;

		public CampaignData(InitialCampaignMessage icm) {
			IsWin = false;
			currentRatio = 0;
			remainingBudget = 0.0;
			
			MyBid = 0.0;
			MyRating = 0.0;
			budget = 0.0;

			id = icm.getId();
			
			targetSegment = icm.getTargetSegment();
			SegmentSize = MarketSegment.marketSegmentSize(this.targetSegment);

			reachImps = icm.getReachImps();
			videoCoef = icm.getVideoCoef();
			mobileCoef = icm.getMobileCoef();

			dayEnd = icm.getDayEnd();
			dayStart = icm.getDayStart();

			ratioTillFinish = reachImps / (dayEnd-dayStart+1);
			impressionRatio = reachImps / (dayEnd-dayStart+1);

			campaignDifficulty = (double) reachImps / ((double) (dayEnd-dayStart+1)*(double)(MarketSegment.marketSegmentSize(targetSegment)));
			stats = new CampaignStats(0, 0, 0);

		}

		public CampaignData(CampaignOpportunityMessage com) {
			IsWin = false;
			currentRatio = 0;
			budget = 0.0;
			remainingBudget = 0.0;
			MyBid = 0.0;
			MyRating = 0.0;

			id = com.getId();
			reachImps = com.getReachImps();

			targetSegment = com.getTargetSegment();
			SegmentSize = MarketSegment.marketSegmentSize(this.targetSegment);;

			videoCoef = com.getVideoCoef();
			mobileCoef = com.getMobileCoef();

			dayStart = com.getDayStart();
			dayEnd = com.getDayEnd();

			ratioTillFinish = reachImps/(dayEnd-dayStart+1);
			impressionRatio = reachImps/(dayEnd-dayStart+1);

			
			campaignDifficulty = (double) reachImps / ((double)(dayEnd-dayStart+1)*(double)(MarketSegment.marketSegmentSize(targetSegment)));
			stats = new CampaignStats(0, 0, 0);
		}

		@Override
		public String toString() {
			return "Campaign ID " + id + ": " + "day " + dayStart + " to "
					+ dayEnd + " " + targetSegment + ", reach: " + reachImps
					+ " coefs: (v=" + videoCoef + ", m=" + mobileCoef + ")";
		}

		/*****************/
		/*$$$ Getters $$$*/
		/*****************/

		public double getvideoCoef() {
			return videoCoef;
		}
		
		public double getmobileCoef() {
			return mobileCoef;
		}		

		public int getId() {
			return id;
		}

		public long getreachImps(){
			return reachImps;
		}
				
		public Integer getSegmentSize(){
			return SegmentSize;
		}		
		
		public double getMyBid(){
			return MyBid;
		}
		
		public double getMyRating(){
			return MyRating;
		}
		
		public double getBudget() {
			return budget;
		}

		public double getImpressionRatio() {
			return impressionRatio;
		}
		
		public double getRemainingBudget() {
			return remainingBudget;
		}

		public double getCampaignDifficulty() {
			return campaignDifficulty;
		}
		
		public double getCurrentRatio() {
			return currentRatio;
		}
		
		public double getRatioTillFinish() {
			return ratioTillFinish;
		}

		public long getLength(){
			return dayEnd - dayStart;
		}

		public long getdayStart() {
			return dayStart;
		}
		
		public long getdayEnd() {
			return dayEnd;
		}
		
		public AdxQuery[] getCampaignQueries() {
			return campaignQueries;
		}

		public AdxQuery[] getcampaignQueries(){
			return campaignQueries; 
		}

		public int impsTogo() {
			return (int) Math.max(0, reachImps - stats.getTargetedImps());
		}
		
		public int daysTogo(int day){
			return (int) dayEnd-day;
		}
		
		public Set<MarketSegment> gettargetSegment() {
			return targetSegment;
		}

		public boolean IsWin(){
			return IsWin;
		}

		/*****************/
		/*$$$ Setters $$$*/
		/*****************/

		public void setMyRating(double rate) {
			MyRating = rate;
		}

		public void setMyBid(double bid) {
			MyBid = bid;
		}

		public void setStats(CampaignStats s) {
			stats.setValues(s);
		}

		public void setRemainingBudget(double d) {
			remainingBudget = d;
		}

		public void setBudget(double d) {
			budget = d;
		}

		public void setCampaignQueries(AdxQuery[] campaignQueries) {
			this.campaignQueries = campaignQueries;
		}
		
		public void setRatios(int day){
			if ((dayEnd>day)&&(day>dayStart)){
				currentRatio = stats.getTargetedImps() / (day-dayStart);
				ratioTillFinish = impsTogo() / daysTogo(day);
				ratioTillFinish = ratioTillFinish + 1;
			}
		}
		
		public void setIsWin(boolean win) {
			IsWin = win;
		}

}

