

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.props.ReservePriceInfo;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.AdNetBidMessage;
import tau.tac.adx.report.demand.AdNetworkDailyNotification;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.CampaignReportKey;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.report.demand.campaign.auction.CampaignAuctionReport;
import tau.tac.adx.report.publisher.AdxPublisherReport;
import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import edu.umich.eecs.tac.props.BankStatus;

import modules.CampaignData;
import modules.CampaignBidder;
import modules.BidBundle;
import modules.UCS_Bidder;

/**
 * 
 * @author Top Three (TOP3)
 * final Agent
 * 
 */
public class TOP3 extends Agent {

	private final Logger log = Logger.getLogger(TOP3.class.getName());

	/*******************/
	/*$$$ Variables $$$*/
	/*******************/

	@SuppressWarnings("unused") //no warnings for unused vars

	/**
	 * Basic simulation information. An agent should receive the {@link
	 * StartInfo} at the beginning of the game or during recovery.
	 **/
	private StartInfo startInfo;

	/**
	 * Messages received:
	 * 
	 * We keep all the {@link CampaignReport campaign reports} delivered to the
	 * agent. We also keep the initialization messages {@link PublisherCatalog}
	 * and {@link InitialCampaignMessage} and the most recent messages and
	 * reports {@link CampaignOpportunityMessage}, {@link CampaignReport}, and
	 * {@link AdNetworkDailyNotification}.
	 */
	private final Queue<CampaignReport> campaignReports;
	private PublisherCatalog publisherCatalog;
	private InitialCampaignMessage initialCampaignMessage;
	private AdNetworkDailyNotification adNetworkDailyNotification;

	/**
	 * The addresses of server entities to which the agent should send the daily
	 * bids data
	 **/
	private String demandAgentAddress;
	private String adxAgentAddress;

	/**
	 * we maintain a list of queries - each characterized by the web site (the
	 * publisher), the device type, the ad type, and the user market segment
	 **/
	private AdxQuery[] queries;


	/**
	 * Information regarding the latest campaign opportunity announced
	 */
	private CampaignData pendingCampaign;


	/**
	 * We maintain a collection (mapped by the campaign id) of the campaigns won
	 * by our agent.
	 */
	private Map<Integer, CampaignData> myCampaigns;

	/*
	 * the bidBundle to be sent daily to the AdX
	 */
	private AdxBidBundle bidBundle;

	public double currUcsBid; //The current bid for the user classification service
	public double lastCampaignBid; //The current bid for the user classification service
	
	/*
	 * current day of simulation
	 */
	
	private int day=0; //current day
	private String[] publisherNames; //active publishers in sim
	private CampaignData currCampaign;

	private CampaignData lastCampaignOpportunity; //Last CampaignOpportunity

	public Map<Integer, CampaignData> ActiveCampaigns = new HashMap<Integer, CampaignData>(); //A map (by campaign id) of all the active campaigns in sim
	
	public Map<Integer, CampaignData> myActiveCampaigns = new HashMap<Integer, CampaignData>(); //A map of all the active campaigns won by agent
	
	

	public TOP3() {
		campaignReports = new LinkedList<CampaignReport>();
	}

	private int numOfCampaigns = 0;  //Number of campaigns won //might be unnecessary.
	private double MyCurrRating=1.0; //Current rating of our agent 

	/******************/
	/*$$$ Handlers $$$*/
	/******************/
	
	@Override
	protected void messageReceived(Message message) {
		try {
			Transportable content = message.getContent();

			// log.fine(message.getContent().getClass().toString());

			if (content instanceof InitialCampaignMessage) {
				handleInitialCampaignMessage((InitialCampaignMessage) content);
			} else if (content instanceof CampaignOpportunityMessage) {
				handleICampaignOpportunityMessage((CampaignOpportunityMessage) content);
			} else if (content instanceof CampaignReport) {
				handleCampaignReport((CampaignReport) content);
			} else if (content instanceof AdNetworkDailyNotification) {
				handleAdNetworkDailyNotification((AdNetworkDailyNotification) content);
			} else if (content instanceof AdxPublisherReport) {
				handleAdxPublisherReport((AdxPublisherReport) content);
			} else if (content instanceof SimulationStatus) {
				handleSimulationStatus((SimulationStatus) content);
			} else if (content instanceof PublisherCatalog) {
				handlePublisherCatalog((PublisherCatalog) content);
			} else if (content instanceof AdNetworkReport) {
				handleAdNetworkReport((AdNetworkReport) content);
			} else if (content instanceof StartInfo) {
				handleStartInfo((StartInfo) content);
			} else if (content instanceof BankStatus) {
				handleBankStatus((BankStatus) content);
			} else if(content instanceof CampaignAuctionReport) { //leave here as a monioment
				hadnleCampaignAuctionReport((CampaignAuctionReport) content);
			} else if (content instanceof ReservePriceInfo) {
				// ((ReservePriceInfo)content).getReservePriceType();
				// this also is unnessecery, but we leave it here
			} else {
				System.out.println("UNKNOWN Message Received: " + content);
			}

		} catch (NullPointerException e) {
			this.log.log(Level.SEVERE,
					"Exception thrown while trying to parse message." + e);
			return;
		}
	}

	private void hadnleCampaignAuctionReport(CampaignAuctionReport content) {
		// ingoring - this message is obsolete
		//this also is unnessecery, but leave it here to not break things
	}



	/**
	 * On day 0, a campaign (the "initial campaign") is allocated to each
	 * competing agent. The campaign starts on day 1. The address of the
	 * server's AdxAgent (to which bid bundles are sent) and DemandAgent (to
	 * which bids regarding campaign opportunities may be sent in subsequent
	 * days) are also reported in the initial campaign message
	 */
	private void handleInitialCampaignMessage(InitialCampaignMessage campaignMessage) {
		System.out.println(campaignMessage.toString());

		day = 0;

		initialCampaignMessage = campaignMessage;
		demandAgentAddress = campaignMessage.getDemandAgentAddress();
		adxAgentAddress = campaignMessage.getAdxAgentAddress();

		CampaignData campaignData = new CampaignData(initialCampaignMessage);
		campaignData.setBudget(initialCampaignMessage.getBudgetMillis()/1000.0);
		campaignData.setRemainingBudget((initialCampaignMessage.getBudgetMillis()/1000.0));
		campaignData.setMyBid(initialCampaignMessage.getBudgetMillis()); //beacause we get the campaign of first day at random, it's like our bid equals the budget.
		campaignData.setIsWin(true);   //we 'win' the first campaign
		campaignData.setMyRating(1.0); //initial rating

		currCampaign = campaignData;
		genCampaignQueries(currCampaign);

		/*
		 * The initial campaign is already allocated to our agent so we add it
		 * to our allocated-campaigns list.
		 */
		System.out.println("Day " + day + ": Allocated campaign - " + campaignData);
		myCampaigns.put(initialCampaignMessage.getId(), campaignData);
		
		numOfCampaigns = 1; 
		myActiveCampaigns.put(initialCampaignMessage.getId(), campaignData);
		ActiveCampaigns.put(initialCampaignMessage.getId(), campaignData);

	}

	private void handleBankStatus(BankStatus content) {
		System.out.println("Day " + day + " :" + content.toString());
	}

	/**
	 * Processes the start information.
	 * 
	 * @param startInfo
	 *            the start information.
	 */
	protected void handleStartInfo(StartInfo startInfo) {
		this.startInfo = startInfo;
	}


	/**
	 * Process the reported set of publishers
	 * 
	 * @param publisherCatalog
	 */
	private void handlePublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
		generateAdxQuerySpace();
		getPublishersNames();

	}

	

	/**
	 * On day n ( > 0) a campaign opportunity is announced to the competing
	 * agents. The campaign starts on day n + 2 or later and the agents may send
	 * (on day n) related bids (attempting to win the campaign). The allocation
	 * (the winner) is announced to the competing agents during day n + 1.
	 */
	private void handleICampaignOpportunityMessage(CampaignOpportunityMessage com) {

		day = com.getDay();

		pendingCampaign = new CampaignData(com);
		System.out.println("Day " + day + ": Campaign opportunity - " + pendingCampaign);

		/*
		 * The campaign requires com.getReachImps() impressions. The competing
		 * Ad Networks bid for the total campaign Budget (that is, the ad
		 * network that offers the lowest budget gets the campaign allocated).
		 * The advertiser is willing to pay the AdNetwork at most 1$ CPM,
		 * therefore the total number of impressions may be treated as a reserve
		 * (upper bound) price for the auction.
		 */
		
		
		long cmpBidMillis = CampaignBidder.getCampaignBid(pendingCampaign,ActiveCampaigns,day,lastCampaignOpportunity,MyCurrRating); //calc the new bid
		lastCampaignBid = cmpBidMillis;
		lastCampaignOpportunity = pendingCampaign;
		lastCampaignOpportunity.setMyBid(lastCampaignBid);

		System.out.println("Day " + day + ": Campaign total budget bid (millis): " + cmpBidMillis);

		/*
		 * Adjust ucs bid s.t. target level is achieved. Note: The bid for the
		 * user classification service is piggybacked
		 */
		
		currUcsBid = UCS_Bidder.calcUCSBid(myActiveCampaigns, day);

		/* Note: Campaign bid is in millis */
		AdNetBidMessage bids = new AdNetBidMessage(currUcsBid, pendingCampaign.getId(), cmpBidMillis);
		sendMessage(demandAgentAddress, bids);
	}

	/**
	 * On day n ( > 0), the result of the UserClassificationService and Campaign
	 * auctions (for which the competing agents sent bids during day n -1) are
	 * reported. The reported Campaign starts in day n+1 or later and the user
	 * classification service level is applicable starting from day n+1.
	 */
	private void handleAdNetworkDailyNotification(AdNetworkDailyNotification notificationMessage) {

		adNetworkDailyNotification = notificationMessage;

		System.out.println("Day " + day + ": Daily notification for campaign "
				+ adNetworkDailyNotification.getCampaignId());

		String campaignAllocatedTo = " allocated to "
				+ notificationMessage.getWinner();
		
		MyCurrRating=notificationMessage.getQualityScore();

        UCS_Bidder.setLastResultLevel(notificationMessage.getServiceLevel());
		

		if ((pendingCampaign.getId() == adNetworkDailyNotification.getCampaignId())
				&& (notificationMessage.getCostMillis() != 0)) {

			/* add campaign to list of won campaigns */
			pendingCampaign.setBudget(notificationMessage.getCostMillis()/1000.0);
			pendingCampaign.setRemainingBudget(notificationMessage.getCostMillis()/1000.0);
			pendingCampaign.setMyBid(lastCampaignBid); //updates bid for the campaign we sent
			pendingCampaign.setIsWin(true); //updates that we won the campaign
			pendingCampaign.setMyRating(MyCurrRating); //initial rating
			currCampaign = pendingCampaign;
			genCampaignQueries(currCampaign);
			myCampaigns.put(pendingCampaign.getId(), pendingCampaign);
			myActiveCampaigns.put(pendingCampaign.getId(), pendingCampaign);
			numOfCampaigns=numOfCampaigns+1; 
			
			campaignAllocatedTo = " WON at cost (Millis)"
					+ notificationMessage.getCostMillis();
		}

		ActiveCampaigns.put(pendingCampaign.getId(), pendingCampaign); //addes to active campaigns list
		
		//removes inactive campaigns & update map
		Campaigns_Update_Remove(); 
		My_Campaigns_Update_Remove(); 
		
		System.out.println("Day " + day + ": " + campaignAllocatedTo
				+ ". UCS Level set to " + notificationMessage.getServiceLevel()
				+ " at price " + notificationMessage.getPrice()
				+ " Quality Score is: " + MyCurrRating);
	}

	/**
	 * The SimulationStatus message received on day n indicates that the
	 * calculation time is up and the agent is requested to send its bid bundle
	 * to the AdX.
	 */
	private void handleSimulationStatus(SimulationStatus simulationStatus) {
		System.out.println("Day " + day + " : Simulation Status Received");
		sendBidAndAds();
		System.out.println("Day " + day + " ended. Starting next day");
		++day;
	}

	/**
	 * Our implementation is at BidBundle.sendBidAndAds
	 */
	protected void sendBidAndAds() {

		bidBundle = BidBundle.sendBidAndAds(myCampaigns, day, ActiveCampaigns );
		if (bidBundle != null) {
			sendMessage(adxAgentAddress, bidBundle);
		}
	}


	/**
	 * Campaigns performance w.r.t. each allocated campaign
	 */
	private void handleCampaignReport(CampaignReport campaignReport) {

		campaignReports.add(campaignReport);

		/*
		 * for each campaign, the accumulated statistics from day 1 up to day
		 * n-1 are reported
		 */
		for (CampaignReportKey campaignKey : campaignReport.keys()) {
			int cmpId = campaignKey.getCampaignId();
			CampaignStats cstats = campaignReport.getCampaignReportEntry(campaignKey).getCampaignStats();
			myCampaigns.get(cmpId).setStats(cstats);

			myCampaigns.get(cmpId).setRatios(day); //will update the ration by the stats
			double remainingBudget = myCampaigns.get(cmpId).getRemainingBudget();
			double newBudget = remainingBudget- cstats.getCost();
			myCampaigns.get(cmpId).setRemainingBudget(newBudget);

			System.out.println("Day " + day + ": Updating campaign " + cmpId + " stats: "
					+ cstats.getTargetedImps() + " tgtImps "
					+ cstats.getOtherImps() + " nonTgtImps. Cost of imps is "
					+ cstats.getCost());

			System.out.println("TOP3: previos budget was for campaign "+cmpId +"was "+remainingBudget);
			System.out.println("TOP3: new budget was for campaign "+cmpId +"is "+newBudget);
		}
	}

	/**
	 * Users and Publishers statistics: popularity and ad type orientation
	 */
	private void handleAdxPublisherReport(AdxPublisherReport adxPublisherReport) {

		//we don't want this report to be printed
		return;
		
		/*
		System.out.println("Publishers Report: ");
		for (PublisherCatalogEntry publisherKey : adxPublisherReport.keys()) {
			AdxPublisherReportEntry entry = adxPublisherReport.getEntry(publisherKey);
			System.out.println(entry.toString());
		}
		*/
	}


	/**
	 * 
	 * @param AdNetworkReport
	 */
	private void handleAdNetworkReport(AdNetworkReport adnetReport) {

		System.out.println("Day " + day + " : AdNetworkReport");
		/*
		 * for (AdNetworkKey adnetKey : adnetReport.keys()) {
		 * 
		 * double rnd = Math.random(); if (rnd > 0.95) { AdNetworkReportEntry
		 * entry = adnetReport .getAdNetworkReportEntry(adnetKey);
		 * System.out.println(adnetKey + " " + entry); } }
		 */
	}

	
	@Override
	protected void simulationSetup() {
		day = 0;
		bidBundle = new AdxBidBundle();

		//We uses better computaion for the ucsbid, of course..
		/* initial bid between 0.1 and 0.2 */
		//ucsBid = 0.1 + random.nextDouble()/10.0;

		myCampaigns = new HashMap<Integer, CampaignData>();
		log.fine("AdNet " + getName() + " simulationSetup");
	}

	
	@Override
	protected void simulationFinished() {
		campaignReports.clear();
		bidBundle = null;
	}

	/**
	 * A user visit to a publisher's web-site results in an impression
	 * opportunity (a query) that is characterized by the the publisher, the
	 * market segment the user may belongs to, the device used (mobile or
	 * desktop) and the ad type (text or video).
	 * 
	 * An array of all possible queries is generated here, based on the
	 * publisher names reported at game initialization in the publishers catalog
	 * message
	 */
	private void generateAdxQuerySpace() {
		if (publisherCatalog != null && queries == null) {
			Set<AdxQuery> querySet = new HashSet<AdxQuery>();

			/*
			 * for each web site (publisher) we generate all possible variations
			 * of device type, ad type, and user market segment
			 */
			for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog) {
				String publishersName = publisherCatalogEntry
						.getPublisherName();
				for (MarketSegment userSegment : MarketSegment.values()) {
					Set<MarketSegment> singleMarketSegment = new HashSet<MarketSegment>();
					singleMarketSegment.add(userSegment);

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.video));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.video));

				}

				/**
				 * An empty segments set is used to indicate the "UNKNOWN"
				 * segment such queries are matched when the UCS fails to
				 * recover the user's segments.
				 */
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.text));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.text));
			}
			queries = new AdxQuery[querySet.size()];
			querySet.toArray(queries);
		}
	}
	
	/*genarates an array of the publishers names
	 * */
	private void getPublishersNames() {
		if (null == publisherNames && publisherCatalog != null) {
			ArrayList<String> names = new ArrayList<String>();
			for (PublisherCatalogEntry pce : publisherCatalog) {
				names.add(pce.getPublisherName());
			}

			publisherNames = new String[names.size()];
			names.toArray(publisherNames);
		}

	//System.out.println("getPublishersNames: " + Arrays.toString(names.toArray()));
	}
	

	
	/*
	 * genarates the campaign queries relevant for the specific campaign, and assign them as the campaigns campaignQueries field 
	 */
	private void genCampaignQueries(CampaignData campaignData) {
		Set<AdxQuery> campaignQueriesSet = new HashSet<AdxQuery>();
		for (String PublisherName : publisherNames) {
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.gettargetSegment(), Device.mobile, AdType.text));
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.gettargetSegment(), Device.mobile, AdType.video));
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.gettargetSegment(), Device.pc, AdType.text));
			campaignQueriesSet.add(new AdxQuery(PublisherName,
					campaignData.gettargetSegment(), Device.pc, AdType.video));
		}

		campaignData.setCampaignQueries(new AdxQuery[campaignQueriesSet.size()]);
		campaignQueriesSet.toArray(campaignData.getCampaignQueries());
		//System.out.println("!!!!!!!!!!!!!!!!!!!!!!"+Arrays.toString(campaignData.getCampaignQueries())+"!!!!!!!!!!!!!!!!");
	}
	
	//////////////////////////////////
	//////////////////////////////////
	
	public static boolean isCampaign_Active(Map<Integer, CampaignData> myActiveCampaigns,int day){
		int dayUCSFor = day;
		dayUCSFor = dayUCSFor +1; //yes
		boolean Ans;
		for (CampaignData campaign : myActiveCampaigns.values()){
			if ((campaign.impsTogo() > 0) && (dayUCSFor >= campaign.getdayStart()) && (dayUCSFor <= campaign.getdayEnd())) { //If the campaign is active
				return true;
			}
		}
		return false;
	}

	private void Campaigns_Update_Remove(){
		Set<Integer> campaignsToRemove = new HashSet<Integer>();

		if (ActiveCampaigns.size()>0){ 
			for (Map.Entry<Integer, CampaignData> campaign : ActiveCampaigns.entrySet()){ //loop over all campaigns
				if (campaign.getValue().getdayEnd()<day){
					campaignsToRemove.add(campaign.getKey());
				}
			}

			if (campaignsToRemove.size()>0){ //If we have campaigns to delete, remove them
				for (Integer campaignID: campaignsToRemove){
					ActiveCampaigns.remove(campaignID);
				}
			}
		}
	}
	
	private void My_Campaigns_Update_Remove() {
		Set<Integer> campaignsToRemove = new HashSet<Integer>();
		if (myActiveCampaigns.size()>0){
			for (Map.Entry<Integer, CampaignData> campaign : myActiveCampaigns.entrySet()){ //loop over all campaigns
				if (campaign.getValue().getdayEnd()<day){
					campaignsToRemove.add(campaign.getKey());
				}
			}

			if (campaignsToRemove.size()>0){ //If we have campaigns to delete, remove them
				for (Integer campaignID: campaignsToRemove){
					myActiveCampaigns.remove(campaignID);
				}
			}
		}
		System.out.println("TOP3: We have " + myActiveCampaigns.size() + "active campaigns at the moment");
	}
}
