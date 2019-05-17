package modules;

import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.report.adn.MarketSegment;
import edu.umich.eecs.tac.props.Ad;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import modules.CampaignData;

/* 
The class will Override the given functionality of SendBidsAndAds(..) 
Will be used in the agent in function handleSimulationStatus() 
Cointains 2 public functions: isCampaignActive(CampaignData campaign,int day) , sendBidAndAds(Map<Integer, CampaignData> myCampaigns, int day, Map<Integer,CampaignData> activeCampaigns)
*/

public class BidBundle 
{

	//as explained above
	public static AdxBidBundle sendBidAndAds(Map<Integer, CampaignData> myCampaigns, int day, Map<Integer,CampaignData> activeCampaigns) 
	{

		Map<CampaignData, Integer> ratios = new HashMap<CampaignData, Integer>(); //map of ratios
		Map< Set<MarketSegment>, Set<CampaignData> > amountBySegments = new HashMap< Set<MarketSegment>, Set<CampaignData> >(); //Amount By Segments
		
		AdxBidBundle bidBundle = new AdxBidBundle();
		
		boolean isAct;
		
		final boolean debugMode = false;
		
		long impsToGo;
		double Impression_Ratio;
		double Ratio_Till_Finish;
		
		long Limit_Impression;
		double Limit_Budget;
		
		//inits
		int RatioSum=0;
		int count_ent = 0;
		double budget = 0;
		double rbid = 0;
		double pi = 0; 

		int panic;

		String str = "";
		
		//inner class parameter tweeks
		double IMP_RARTIO_MULT     = 1.25;   //ratio for classic campaigns
 		double IMP_RARTIO_MULT2    = 1.125;  //ratio for short campaigns
		double IMP_RARTIO_MED      = 1.06;  //ratio for meduim campaigns
		double IMP_RARTIO_LONG     = 1.027; //ratio for long campaigns
		double BUDGET_REM_1        = 0.55; //determains the percent of the budget can be spent in each part of the game
		double BUDGET_REM_2        = 0.4;  //going down in time
		double BUDGET_REM_3        = 0.35;
		double BUDGET_REM_4        = 0.15;
		double BUDGET_REM_5        = 0.5;  //in the end

		//game limits (can tweek also)
		final Double BB_limit = 1.1;  //limits for bids
		final Double BB_limit2 = 1.5;
		final Double BB_limit3 = 1.3;
		final Integer RTO_Panic = 5;  //panic ratio for 
		
		//this sort campaigns by segments
		for (CampaignData this_camp : myCampaigns.values()) 
		{
			isAct = isCampaignActive(this_camp, day);
			if (isAct) 
			{
				Set<MarketSegment> key = this_camp.gettargetSegment();
				Set<CampaignData> amo = amountBySegments.get(key); 
				if (amo == null) 
				{
					amo = new HashSet<CampaignData>();
					amountBySegments.put(key, amo);
				}
				amo.add(this_camp);
				amountBySegments.put(key, amo);
				ratios.put(this_camp, 1);
			}
		}
		
		//Initializing weights for the campaigns
		for( Set<MarketSegment> this_seg : amountBySegments.keySet())
		{
			Set<CampaignData> amo = amountBySegments.get(this_seg);
			Iterator<CampaignData> iter=amo.iterator();
			if ((amo.size() >= 1) && (amo.size() <= 2))
			{
				continue;
			}
			else
			{	
				RatioSum=0;
				while(iter.hasNext())
				{
					CampaignData camp = iter.next();
					RatioSum += camp.getRatioTillFinish();
				}
				for(CampaignData camp : amo)
				{
					ratios.put(camp, (int)((camp.getRatioTillFinish() * 10) / RatioSum));
				}
			}	
		}
		
		
		//this is the main loop on myCampaigns 
		for (CampaignData campaign : myCampaigns.values()) 
		{
			
			isAct = isCampaignActive(campaign, day);
			if (isAct) 
			{ 
				if (campaign.daysTogo(day) <= 0){
					continue;
				}
				
				Ratio_Till_Finish = campaign.getRatioTillFinish();
				Impression_Ratio = campaign.getImpressionRatio();
				impsToGo = campaign.impsTogo();
				
				panic = 0;
				count_ent = 0;
				
				String camp_id      = Integer.toString(campaign.id);
				String camp_rem_bud = Double.toString(campaign.getRemainingBudget());
				if (debugMode){
					System.out.println("[debug BidBundle] Campaign " + camp_id + " - Imps To Go: " + impsToGo + " Remaining Budget: " + camp_rem_bud);
				}
				
				for (AdxQuery query : campaign.getCampaignQueries()) 
				{
					pi = CampaignBidder.getSegmentRatioByDay(query.getMarketSegments(), day, activeCampaigns);
					if(day <= 5)
					{
						pi += 0.5;
						
						if (day != 1)
						{
							budget = campaign.getRemainingBudget() * BUDGET_REM_1;
							if (Impression_Ratio <= Ratio_Till_Finish)
							{
								double const_limit = BB_limit;
								budget = budget / const_limit;
							}
							else
							{
								//str = "[debug BidBundle] ratio not good!! (first day)";
								
								if (Impression_Ratio * IMP_RARTIO_MULT > Ratio_Till_Finish)
								{
									double const_limit = BB_limit;
									budget = const_limit * budget;
									
								}
								else
								{
									double const_limit = BB_limit;
									double const_limit2 = BB_limit2;
									
									panic = 1;
									//str = "[debug BidBundle] PANIC!!! (first days)";
									
									budget = budget * const_limit * const_limit2;
									int temp = ratios.get(campaign);
									ratios.put(campaign, RTO_Panic + temp);
									
								}
									
							}
								
						}
							
						else
						{
							budget = campaign.getRemainingBudget() * BUDGET_REM_5;
						}
					}
					
					else if (campaign.getCampaignDifficulty() > 0.75)
					{
						budget = campaign.getRemainingBudget() * BUDGET_REM_2;
						
						if(campaign.getLength() == 5)
						{
							if (Impression_Ratio >= Ratio_Till_Finish)
							{
								budget = budget / BB_limit;
									
							}
							else
							{
								
								//str = "[debug BidBundle] campaign ratio not good (MEDIUM)";
								if (Impression_Ratio * IMP_RARTIO_MED > Ratio_Till_Finish)
								{
									double const_limit = BB_limit;
									budget = const_limit * budget;
									
								}
								else{
									panic = 1;
									//str = "[debug BidBundle] PANIC!!! (MEDIUM campaign)";
									
									double const_limit = BB_limit;
									double const_limit2 = BB_limit2;
									
									budget = budget * const_limit * const_limit2;
									int temp = ratios.get(campaign);
									ratios.put(campaign, RTO_Panic + temp);
									
								}
								
							}
						}
						
						else if(campaign.getLength() == 3)
						{
							if (Impression_Ratio >= Ratio_Till_Finish)
							{
								double const_limit = BB_limit;
								budget = budget / const_limit;
									
							}
							else {
								//str = "[debug BidBundle] campaign ratio not good (SHORT)";
								if (Impression_Ratio * IMP_RARTIO_MULT2 > Ratio_Till_Finish)
								{
									double const_limit = BB_limit;
									budget = budget * const_limit;
									
								}
								else{
									panic = 1;
									//str = "[debug BidBundle] PANIC!!! (SHORT campaign)";
									
									double const_limit = BB_limit;
									double const_limit2 = BB_limit2;
									
									budget = budget * const_limit * const_limit2;
									int temp = ratios.get(campaign);
									ratios.put(campaign, RTO_Panic + temp);
									
								}
								
							}
								
						}
			
						
						else
						{
							if (Impression_Ratio >= Ratio_Till_Finish)
							{
								double const_limit = BB_limit;
								budget = budget / const_limit;
								
							}
							else{
								
								//str = "[debug BidBundle] ratio not good (LONG)";
								
								if (Impression_Ratio * IMP_RARTIO_LONG > Ratio_Till_Finish )
								{
									double const_limit = BB_limit;
									budget = budget * const_limit;
									
								}
								else
								{
									panic = 1;
									//str = "[debug BidBundle] PANIC!!! (LONG campaign)";
									
									double const_limit = BB_limit;
									double const_limit2 = BB_limit2;
									
									budget = budget * const_limit * const_limit2;
									int temp = ratios.get(campaign);
									ratios.put(campaign, RTO_Panic + temp);
									
								}
									
							}
								
						}

					}
					
					else if(campaign.getLength() == 3)
					{
						panic = 1;
						budget = campaign.getRemainingBudget() * BUDGET_REM_3;
						
						if (Impression_Ratio >= Ratio_Till_Finish )
						{
							double const_limit = BB_limit;
							budget = budget / const_limit;
							
						}
						
						else
						{
							//str = "[debug BidBundle] campaign ratio not good (SHORT)";
							
							if (Impression_Ratio * IMP_RARTIO_MULT > Ratio_Till_Finish)
							{
								double const_limit = BB_limit;
								budget = budget * const_limit;
							}
							else
							{
								//str = "[debug BidBundle] PANIC!!! (SHORT campaign)";
								
								double const_limit = BB_limit;
								double const_limit2 = BB_limit2;
								
								budget = budget * const_limit * const_limit2;
								
							}
							
							
						}
						
						int temp = ratios.get(campaign);
						ratios.put(campaign, RTO_Panic + temp);
					}
					
					else
					{
						budget = campaign.getRemainingBudget() * BUDGET_REM_4;
						
						if (Impression_Ratio >= Ratio_Till_Finish)
						{
							
							double const_limit = BB_limit;
							budget = budget / const_limit;
							
						}
						else
						{
							//str = "[debug BidBundle] campaign ratio not good (CLASSIC)";
							
							if (Impression_Ratio * IMP_RARTIO_MULT > Ratio_Till_Finish )
							{
								panic = 1;
								//str = "[debug BidBundle] PANIC (CLASSIC campaign)";
								
								double const_limit = BB_limit;
								double const_limit2 = BB_limit2;
								budget = budget * const_limit * const_limit2;
								
								int temp = ratios.get(campaign);
								ratios.put(campaign, RTO_Panic + temp);
								
							}
							else
								budget = budget * BB_limit;
						}
							
					}
					
					//rbid initializaion
					rbid = budget / impsToGo ; 
					
					if (campaign.impsTogo() > count_ent) 
					{
						double tbid = rbid; 
						if (query.getDevice() != Device.pc) 
						{
							count_ent++;
							
							if (AdType.text != query.getAdType()) 
							{
								tbid = rbid * (campaign.getmobileCoef() + campaign.getvideoCoef());
							} 
							else 
							{
								tbid = rbid * campaign.getmobileCoef();
							}

						} 
						
						else 
						{

							count_ent++;
							
							if (AdType.text != query.getAdType())
							{
								tbid = rbid * campaign.getvideoCoef();
							}
							
						}
						
						tbid = 1000 * tbid * pi;
                        if (day<5){
                            // in the first days there are many many campaigns- so in order to complete ours, we ned to be arresive on each impression
                            tbid = tbid * 1.3;
                        }
						
						bidBundle.addQuery(query, tbid, new Ad(null), campaign.getId(), ratios.get(campaign));

					}
				}
				
				String camp_id2 = Integer.toString(campaign.getId());
				if (debugMode){
					System.out.println("[debug BidBundle] Campaign " + camp_id2 + " Day: " + day + " PI: " + pi + " [!]");
					System.out.println(str);
				}
				
				if (panic == 0)
				{
					Limit_Impression = impsToGo;
					Limit_Budget =  budget;

				}
				else
				{
					double const_bud_limit = BB_limit3;
					Limit_Impression = impsToGo;
					Limit_Budget = budget * const_bud_limit;
				}
				
				if (debugMode){
					System.out.println("[debug BidBundle] Day: " + day + " Campaign id: " + camp_id2 + " ImpsLimit: " + Limit_Impression +	" BudgetLimit: " + Limit_Budget);
					System.out.println("[debug BidBundle] Day " + day + ": Updated " + count_ent + " entries for Campaign id " + camp_id2);
				}

				bidBundle.setCampaignDailyLimit(campaign.getId(), (int)Limit_Impression, Limit_Budget);

			}
		}
		
		return bidBundle;
	}
	
	
	//returns boolean answer if giving campaign is active
	public static boolean isCampaignActive(CampaignData campaign, int day) 
	{
		int bid_for_day; 
		bid_for_day = day + 1;
		
		if ((bid_for_day <= campaign.getdayEnd()) && (bid_for_day >= campaign.getdayStart()))
		{
			if (campaign.impsTogo() <= 0)
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		
		else
		{
			return false;
		}
		
	}
}
