/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intelligrated.ruleEngine.domain;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.definition.type.PropertyReactive;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.intelligrated.cacheservice.domainservices.cache.LocationCache;
import com.intelligrated.cacheservice.domainservices.cache.PutawayCache;
import com.intelligrated.entities.inventory.AllocatedInventory;
import com.intelligrated.entities.location.Location;

/**
 * @author sachin.subhedar
 */
@SuppressWarnings("unused")
@PropertyReactive
public class ActionManager {
	private final Logger logger = LogManager.getLogger();
    private String callingServiceName;
    private String message = "No action taken.";
    private String activityType;
    private HashSet<String> actionSet = new HashSet<>(); // needed for all constructors
    BigInteger locationId;
    List<AllocatedInventory> allocatedInventories;
    private final Map<Integer, SortEnum> inventoriesSort = new HashMap<>();
    private Map<String, String> outboundAisleLevel = new HashMap<>();
    private Map<String, String> liftAisleBay = new HashMap<>();
	List<Location> filteredLocations;
    private Map<String, Integer> aisleToEmptyLocations = new HashMap<>();
    String queueName;
    private List<String> lpnList = new ArrayList<>();
    RestTemplate restTemplate;
	List<PutawayCache> areasContainingAnySku = new ArrayList<PutawayCache>();
	List<PutawayCache> areasNotContainingAnySku = new ArrayList<PutawayCache>();
	List<PutawayCache> areasWithMinQty = new ArrayList<PutawayCache>();
	private PutawayCache filteredArea=null;
    private Integer absBayDiff = 0;  
    private Integer bayDiff = Integer.MAX_VALUE;
    private final List<BigInteger> availableLocations = new ArrayList<BigInteger>();;
    public List<LocationCache> unreservedLocations = new ArrayList<LocationCache>();
	
    public void addAction(final String actionString) {
        actionSet.add(actionString);
    }

    public String getCallingServiceName() {
        return callingServiceName;
    }

    public void setCallingServiceName(final String callingServiceName) {
        this.callingServiceName = callingServiceName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public ActionManager(final String callingServiceName) {
        super();
        this.callingServiceName = callingServiceName;
    }
    
    public ActionManager(final RestTemplate restTemplate) {
        super();
        this.restTemplate = restTemplate;
    }

    public ActionManager(final String callingServiceName, final String activityType) {
        this.callingServiceName = callingServiceName;
        this.activityType = activityType;
    }

    public ActionManager() {
        super();
    }

    public String approve(final Activity activity) {
        String status;
        try {
            if (activity.getType() == null) {
                activity.setType(activityType);
            }
            final String uri = callingServiceName + "/activity";
            System.out.println("Activity url: " + uri);
            final RestTemplate restTemplate = new RestTemplate();

            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            final HttpEntity<Activity> entity = new HttpEntity<>(activity, headers);
            ResponseEntity<Activity> result;
            result = restTemplate.exchange(uri, HttpMethod.PUT, entity, Activity.class);
            status = result.getStatusCode().name();
        } catch (NullPointerException | RestClientException e) {
            status = e.getMessage();
        }
        return status;
    }

    public void addInventory(final AllocatedInventory allocatedInventory) {
        if (this.allocatedInventories == null) {
            this.allocatedInventories = new ArrayList<>();
        }
        allocatedInventories.add(allocatedInventory);
    }

    public HashSet<String> getActionSet() {
        return actionSet;
    }

    public void setActionSet(final HashSet<String> actionSet) {
        this.actionSet = actionSet;
    }

    public BigInteger getLocationId() {
        return locationId;
    }

    public void setLocationId(final BigInteger locationId) {
        this.locationId = locationId;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(final String queueName) {
        this.queueName = queueName;
    }
	
	public void setFilteredLocations(final List<Location> locations) {
        this.filteredLocations = locations;
	}
	
	public List<Location> getFilteredLocations() {
        return filteredLocations;
    }
	
	public void addLocation(final Location location) {
        if (this.filteredLocations == null) {
            this.filteredLocations = new ArrayList<>();
        }
        filteredLocations.add(location);
    }
    

    
    public void sortInventories() {
        if ((allocatedInventories != null) && (!allocatedInventories.isEmpty())
                && !this.inventoriesSort.isEmpty()) {
            final Map<Integer, SortEnum> finalInventoriesSort = this.getInventoriesSort();
            final Map<String, String> localOutboundAisleLevel = this.getOutboundAisleLevel();
            final Map<String, String> localLiftAisleBay = this.getLiftAisleBay();

            allocatedInventories.sort((i1, i2) -> {
                final List<Integer> compares = new ArrayList<>();

                final Map<Integer, SortEnum> localInventoriesSort = new HashMap<>(finalInventoriesSort);

                while (!localInventoriesSort.isEmpty()) {
                    // Pick the lowest seq number
                    Map.Entry<Integer, SortEnum> bestEntry = null;
                    for (final Map.Entry<Integer, SortEnum> entry : localInventoriesSort.entrySet()) {
                        if ((bestEntry == null) || (entry.getKey() < bestEntry.getKey())) {
                            bestEntry = entry;
                        }
                    }

                    // Use the SortEnum from the best entry to create a new entry for the compares list.
                    // The compares list will then be used at the end to do the sort.
                    if (bestEntry != null) {
                        switch (bestEntry.getValue()) {
                        	case UNALLOCATED_QTY: {
                        		compares.add(Integer.compare(i1.getQty() - i1.getAllocatedQuantity(), i2.getQty() - i2.getAllocatedQuantity()));
                        	}
                        	break;
                            case FIFO: {
                                // Use the LastPutDateTime, but remove the minutes, seconds, and nanoseconds so that Inventory's are grouped by hour.
                                LocalDateTime date1 = LocalDateTime.MIN, date2 = LocalDateTime.MIN;

                                if (i1.getContainerPutAwayDateTime() != null) {
                                    date1 = i1.getContainerPutAwayDateTime();
                                }
                                if (i2.getContainerPutAwayDateTime() != null) {
                                    date2 = i2.getContainerPutAwayDateTime();
                                }

                                compares.add(date1.compareTo(date2));
                            }
                            break;
                            case FEFO: {
                                LocalDateTime date1 = LocalDateTime.MIN, date2 = LocalDateTime.MIN;

                                if (i1.getExpireDateTime() != null) {
                                    date1 = i1.getExpireDateTime();
                                }
                                if (i2.getExpireDateTime() != null) {
                                    date2 = i2.getExpireDateTime();
                                }

                                compares.add(date1.compareTo(date2));
                            }
                            break;
                            case CLOSEST_TO_OUTBOUND_LEVEL: {
                                long level1 = 0, level2 = 0, outboundlevel1 = 0, outboundlevel2 = 0;
                                String temp;

                                temp = localOutboundAisleLevel.get(i1.getContainer().getLocation().getAisle());
                                if (temp != null) {
                                    outboundlevel1 = Long.parseLong(temp);
                                }
                                temp = localOutboundAisleLevel.get(i2.getContainer().getLocation().getAisle());
                                if (temp != null) {
                                    outboundlevel2 = Long.parseLong(temp);
                                }

                                if ((i1.getContainer() != null) && (i1.getContainer().getLocation() != null)) {
                                    level1 = Math.abs(outboundlevel1 - Long.parseLong(i1.getContainer().getLocation().getLevel()));
                                }
                                if ((i2.getContainer() != null) && (i2.getContainer().getLocation() != null)) {
                                    level2 = Math.abs(outboundlevel2 - Long.parseLong(i2.getContainer().getLocation().getLevel()));
                                }

                                compares.add(Long.compare(level1, level2));
                            }
                            break;
                            case CLOSEST_TO_LIFT_BAY: {
                                long bay1 = 0, bay2 = 0, liftBay1 = 0, liftBay2 = 0;
                                String temp;

                                temp = localLiftAisleBay.get(i1.getContainer().getLocation().getAisle());
                                if (temp != null) {
                                    liftBay1 = Long.parseLong(temp);
                                }
                                temp = localLiftAisleBay.get(i2.getContainer().getLocation().getAisle());
                                if (temp != null) {
                                    liftBay2 = Long.parseLong(temp);
                                }

                                if ((i1.getContainer() != null) && (i1.getContainer().getLocation() != null)) {
                                    bay1 = Math.abs(liftBay1 - Long.parseLong(i1.getContainer().getLocation().getBay()));
                                }
                                if ((i2.getContainer() != null) && (i2.getContainer().getLocation() != null)) {
                                    bay2 = Math.abs(liftBay2 - Long.parseLong(i2.getContainer().getLocation().getBay()));
                                }

                                compares.add(Long.compare(bay1, bay2));
                            }

                            // Remove the map entry.
                            localInventoriesSort.remove(bestEntry.getKey());
                            break;
                            default:
                                break;
                        }
                    }
                }
                // Do the sort by returning the first non-zero compare.
                // So, if the first criterion is equal, use the secondary.  If the second is equal, use the third.  And so on...
                for (final Integer compare : compares) {
                    if (!compare.equals(0)) {
                        return compare;
                    }
                }
                return 0;
            });
        }
    }

    public List<AllocatedInventory> getAllocatedInventories() {
        return allocatedInventories;
    }

    public void setAllocatedInventories(final List<AllocatedInventory> inventories) {
        this.allocatedInventories = inventories;
    }

    public Map<String, Integer> getAisleToEmptyLocations() {
        return aisleToEmptyLocations;
    }

    public void setAisleToEmptyLocations(final Map<String, Integer> aisleToEmptyLocations) {
        this.aisleToEmptyLocations = aisleToEmptyLocations;
    }

    public void addAisleToEmptyLocationsMap(final String aisle) {
        Integer count = this.aisleToEmptyLocations.get(aisle);
        if (count == null) {
            count = 0;
        }
        this.aisleToEmptyLocations.put(aisle, count + 1);
    }

    public boolean isMostAvailableAisle(final String aisle) {

        final Integer aisleCount = this.aisleToEmptyLocations.get(aisle);

        for (final Map.Entry<String, Integer> entry : this.aisleToEmptyLocations.entrySet()) {
            if ((!entry.getKey().equals(aisle)) && (entry.getValue().compareTo(aisleCount) > 0)) {
                return false;
            }
        }
        return true;
    }

    public Map<Integer, SortEnum> getInventoriesSort() {
        return this.inventoriesSort;
    }

    public void setInventoriesSort(final Integer seq, final SortEnum sort) {
        this.inventoriesSort.put(seq, sort);
    }

    public Map<String, String> getOutboundAisleLevel() {
        return outboundAisleLevel;
    }

    public void setOutboundAisleLevel(final Map<String, String> outboundAisleLevel) {
        this.outboundAisleLevel = outboundAisleLevel;
    }

    public void addOutboundAisleLevel(final String aisle, final String level) {
        this.outboundAisleLevel.put(aisle, level);
    }

    public Map<String, String> getLiftAisleBay() {
        return liftAisleBay;
    }

    public void setLiftAisleBay(final Map<String, String> liftAisleBay) {
        this.liftAisleBay = liftAisleBay;
    }

    public void addLiftAisleBay(final String aisle, final String bay) {
        this.liftAisleBay.put(aisle, bay);
    }

    public String getAllActions() {
        String actions = "";
        for (final String action : actionSet) {
            actions += action + " ";
        }
        return actions;
    }
    
    String searchParam="";
	public String getSearchParam() {
		return searchParam;
	}
	public void setSearchParam(final String searchParam) {
		this.searchParam = searchParam;
	}
	
	private List<ContainerLayoutDTO> containerList = new ArrayList<ContainerLayoutDTO>();
	public List<ContainerLayoutDTO> sortedContainerList;
    public List<ContainerLayoutDTO> getContainerList() {
		return containerList;
	}
	public void setContainerList(final List<ContainerLayoutDTO> containerList) {
		this.containerList = containerList;
	}
	
	/**
	 * Description: To get the search criteria string based on the search criteria rules triggered
	 * @param String searchparamName, String searchparamValue
	 * @return null
	 */
    public void getPutawaySearchCriteria(final String searchparamName, final String searchparamValue){
    	final StringBuilder searchParamBuilder = new StringBuilder();
    	if(searchParam.equals("")){
    		searchParamBuilder.append(searchparamName)
    		.append("=")
    		.append(searchparamValue);
    	}else{
    		searchParamBuilder.append(searchParam)
    		.append("&")
    		.append(searchparamName)
    		.append("=")
    		.append(searchparamValue);
    	}
    	searchParam=searchParamBuilder.toString();
    }
    
    /**
	 * Description: To get the list of container objects based on the putaway search criteria
	 * @param  null
	 * @return null
	 */
    public void retrievePutawayDonorTotes() {
        String status="";
        try {
			final String uri ="http://" + "DOMAIN-SERVICES" +"/domainServices/containers/containerForInventoryStorage?location.putEnabled=true&location.online=true&" + searchParam;
            final HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            final HttpEntity<Activity> entity = new HttpEntity<>(headers);
            logger.info("Calling DS to retrieve containers for putaway uri: {}",uri);
            containerList = Arrays.asList(restTemplate.getForObject(uri, ContainerLayoutDTO[].class));
           
        } catch (RestClientException e) {
        	logger.error("Could not get containers from domain service, Cause {} ",e.getMessage());
            status = e.getMessage();
        }catch(final Exception e){
        	 status = e.getMessage();
        }
    }
    
    public void addToSortedContainerList(ContainerLayoutDTO container) {
    	if(sortedContainerList == null) {
    		sortedContainerList= new ArrayList<ContainerLayoutDTO>();
    	}
    	sortedContainerList.add(container);
    }
    
    public void addToSortedContainerListWithSKU(ContainerLayoutDTO container, String sku) {
    	if(sortedContainerList == null) {
    		sortedContainerList= new ArrayList<ContainerLayoutDTO>();
    	}
    	
		if(container !=null && container.getCompartmentsInContainer() != null && container.getCompartmentsInContainer().size() >0){
			container.getCompartmentsInContainer().parallelStream()
			.filter(compartment ->  container.getInventoryInContainer().parallelStream().anyMatch(inventory -> ! inventory.getSku().equalsIgnoreCase(sku)) )
			.map(p -> { 
				container.getCompartmentsInContainer().remove(p);
				return p;	
			});
			sortedContainerList.add(container);
		}else{
			if (container != null && container.getInventoryInContainer().parallelStream().anyMatch(inventory -> inventory.getSku().equalsIgnoreCase(sku))) {
				sortedContainerList.add(container);
			}
		}
    }
    public List<String> getLpnList() {
        return lpnList;
    }

    public void setLpnList(List<String> lpnList) {
        this.lpnList = lpnList;
    }
    
    public void addLPN(String lpn) {
        lpnList.add(lpn);
    }
    
	public void computeAreasNotContainingAnyToteSku(List<SkuQuantityDetails> skuDetails, PutawayCache area) {
		if (area.getSkuToQuantityMap() == null) {
			areasNotContainingAnySku.add(area);
		} else {
			List<String> toteSku = skuDetails.parallelStream().map(SkuQuantityDetails::getSku).collect(Collectors.toList());
			area.getSkuToQuantityMap().keySet().retainAll(toteSku);
			if (area.getSkuToQuantityMap().keySet().size() == 0) {
				areasNotContainingAnySku.add(area);
			}
		}
	}

	public List<PutawayCache> getAreasNotContainingAnySku() {
		return this.areasNotContainingAnySku;
	}

	public List<PutawayCache> getAreasWithMinQty() {
		return areasWithMinQty;
	}

	public void setAreasWithMinQty(PutawayCache areasWithMinQty) {
		this.areasWithMinQty.add(areasWithMinQty);
	}

	public void computeAreaHavingMinQty(String maxQtySKU) {
		List<BigInteger> areas = new ArrayList<BigInteger>();
		// Logic to retrieve area for tote SKU which has max qty is part of Area
		// which has least qty.

		if (areasContainingAnySku.size() == 1) {
            setFilteredArea(areasContainingAnySku.get(0));
        } else{
			// Compare this to Area that has least quantity
			Integer minQty = Integer.MAX_VALUE;
			Map<PutawayCache, Integer> cacheQtyMap = new HashMap<PutawayCache, Integer>();
			for (PutawayCache cacheObject : areasContainingAnySku) {
				if (cacheObject.getSkuToQuantityMap().get(maxQtySKU) == null) {
					setAreasWithMinQty(cacheObject);
				} else if (cacheObject.getSkuToQuantityMap().get(maxQtySKU) != null
						&& minQty >= cacheObject.getSkuToQuantityMap().get(maxQtySKU)) {
					minQty = cacheObject.getSkuToQuantityMap().get(maxQtySKU);
					cacheQtyMap.put(cacheObject, cacheObject.getSkuToQuantityMap().get(maxQtySKU));
				}
			}

			if (!(getAreasWithMinQty().size() > 0)) {
				for (Entry<PutawayCache, Integer> entry : cacheQtyMap.entrySet()) {
					if (entry.getValue().equals(minQty)) {
						setAreasWithMinQty(entry.getKey());
					}
				}
			}
        }
	}

	public void computeAreaWithLeastContainers(List<PutawayCache> areas) {
		if(areas.size() == 1) {
			setFilteredArea(areas.get(0));
		} else if (areas.size() > 1) {
			PutawayCache area = areas.stream().filter(e -> e.getNoOfContainers() != null)
					.min(Comparator.comparingInt(PutawayCache::getNoOfContainers)).get();
			// Set the least containers area as final filtered area
			setFilteredArea(area);
		}
	}

	public List<PutawayCache> getAreasContainingAnySku() {
		return this.areasContainingAnySku;
	}
	
	public void setAreasContainingAnySku(List<PutawayCache> areasContainingAnySku) {
		this.areasContainingAnySku = areasContainingAnySku;
	}

	public PutawayCache getFilteredArea() {
		return filteredArea;
	}

	public void setFilteredArea(PutawayCache filteredArea) {
		this.filteredArea = filteredArea;
	}	
	

    public List<BigInteger> getAvailableLocations() {
        return this.availableLocations;
    }
	
    public void selectLocationClosestToPutaway(Set<Object> reservedLocations) {
        if (this.filteredArea == null) {
            return;
        }
        List<LocationCache> liftLocations = this.filteredArea.getLiftLocations();
        List<LocationCache> emptyLocations = this.filteredArea.getEmptyLocations();
        unreservedLocations = emptyLocations.stream()
                .filter(e -> !reservedLocations.contains(e.getId()))
                .collect(Collectors.toList());

        Collections.shuffle(liftLocations);
        Collections.shuffle(unreservedLocations);
        try {
            liftLocations.stream().forEach(ll -> {
                unreservedLocations.stream().forEach(el -> {
                    this.absBayDiff = Math.abs(Integer.parseInt(ll.getBay()) - Integer.parseInt(el.getBay()));

                    if (this.bayDiff >= this.absBayDiff) {
                        this.bayDiff = this.absBayDiff;
                        this.availableLocations.add(0, el.getId());
                    }
                });
            });
        } catch (NumberFormatException ex) {
            logger.error("Not able to found absolute bay difference,wrond values of bay{} ", ex.getMessage());
        }
    }
    
    /**
     * Get exactly one aisle, having least container count among aisles
     * @return String
     */
    public String getAisleforLeastContainerCount(Map<String, Integer> map) {

    	String aisleName = null;
 	    int minValue = Integer.MAX_VALUE;
 	    for(String aisleKey : map.keySet()) {
 	        int value = map.get(aisleKey);
 	        if(value < minValue) {
 	            minValue = value;
 	            aisleName = aisleKey;
 	        }
 	    }
    	
 	    return aisleName;
    }
    ///2nd
    /**
     * Get exactly one aisle(which will be easily accessible) && having least containers count Among aisles(if more than one)
     * @return String
     */
    public String findShortestAccessibleAisle(List<String> aisleList) {
		String shortest = aisleList.get(0);

        for(String str : aisleList) {
            if (str.compareTo(shortest) < 0) {
                shortest = str;
            }
        }
        logger.info("The shortest Aisle string is : " + shortest);
        
        return shortest;
	}
}
