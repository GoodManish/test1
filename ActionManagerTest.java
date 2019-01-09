

public class ActionManagerTest {

	@Test
	public void testComputeAreasNotContainingAnyToteSku() {
		// Create tote SKU list
		ActionManager manager = new ActionManager();
		SkuQuantityDetails skuQuantityDetails1=new SkuQuantityDetails("SKU1", 10);
		SkuQuantityDetails skuQuantityDetails2=new SkuQuantityDetails("SKU2", 20);
		SkuQuantityDetails skuQuantityDetails3=new SkuQuantityDetails("SKU3", 30);
		
		List<SkuQuantityDetails> toteSKUs = new ArrayList<SkuQuantityDetails>();
		toteSKUs.add(skuQuantityDetails1);
		toteSKUs.add(skuQuantityDetails2);
		toteSKUs.add(skuQuantityDetails3);
		
		PutawayCache area = new PutawayCache();
		Map<String, Integer> skuMap = new HashMap<String, Integer>();
		skuMap.put("SKU4", 20);
		skuMap.put("SKU5", 40);
		area.setSkuToQuantityMap(skuMap);
		manager.computeAreasNotContainingAnyToteSku(toteSKUs, area);
		assertEquals(manager.getAreasNotContainingAnySku().size(), 1);
	}

	private List<PutawayCache> getPutawayCacheObjects() {
		PutawayCache area1 = new PutawayCache();
		area1.setAreaId(new BigInteger("1"));
		PutawayCache area2 = new PutawayCache();
		area2.setAreaId(new BigInteger("2"));
		PutawayCache area3 = new PutawayCache();
		area3.setAreaId(new BigInteger("3"));

		// Create SKU to Quantity Maps
		Map<String, Integer> skuMap1 = new HashMap<String, Integer>();
		Map<String, Integer> skuMap2 = new HashMap<String, Integer>();
		Map<String, Integer> skuMap3 = new HashMap<String, Integer>();

		skuMap1.put("SKU1", 10);
		skuMap1.put("SKU2", 20);
		skuMap1.put("SKU3", 30);

		skuMap2.put("SKU1", 30);
		skuMap2.put("SKU2", 20);
		skuMap2.put("SKU3", 10);

		skuMap3.put("SKU1", 20);
		skuMap3.put("SKU2", 10);
		skuMap3.put("SKU3", 30);

		area1.setSkuToQuantityMap(skuMap1);
		area2.setSkuToQuantityMap(skuMap2);
		area3.setSkuToQuantityMap(skuMap3);

		area1.setNoOfContainers(10);
		area2.setNoOfContainers(20);
		area3.setNoOfContainers(30);
		List<PutawayCache> areas = new ArrayList<PutawayCache>();
		areas.add(area1);
		areas.add(area2);
		areas.add(area3);
		return areas;
	}
	
	/**
	 * WES-46483
	 * 
	 * Identify/Retrieve a Specific Aisle from [Aisle-ContainerCount] Map.
	 * 
	 * @return String
	 */
	@Test
	public void testComputeAisleWithLeastContainers() {
	   /* //List will hold 1 container OR more than one Container (in case of Multiple Aisle having Same no of Containers)
		List<Container> containerList = new ArrayList<>(); // For FUTURE Story.
		*/

		// Creating/adding MOCk Map Object from CacheService
		HashMap<String,Integer> mockAisleToContainerCountOBJ = new HashMap<String, Integer>();
       
		List<String> aisleList = new ArrayList<String>();
        mockAisleToContainerCountOBJ.put("AISLE1", 50);
        mockAisleToContainerCountOBJ.put("AISLE2", 10);
        mockAisleToContainerCountOBJ.put("AISLE3", 10);
        mockAisleToContainerCountOBJ.put("AISLE4", 10);
        mockAisleToContainerCountOBJ.put("AISLE5", 40);
        
        int maxValueInMap = (Collections.min(mockAisleToContainerCountOBJ.values()));
        
        for (Entry<String, Integer> entry : mockAisleToContainerCountOBJ.entrySet()) { 
            if (entry.getValue() == maxValueInMap) {
                aisleList.add(entry.getKey());
            }
        }
		
		ActionManager manager = new ActionManager();
		String aisleName = manager.findShortestAccessibleAisle(aisleList);
		
		System.out.println("Aisle Containing least no of containers: "+aisleName);
		
		assertEquals(manager.findShortestAccessibleAisle(aisleList), "AISLE2");
		assertNotEquals(manager.findShortestAccessibleAisle(aisleList), "AISLE4");
		assertNotEquals(manager.findShortestAccessibleAisle(aisleList), "AISLE5");
	}

	@Test
	public void testComputeAreaHavingMinQty() {

		List<PutawayCache> areas = getPutawayCacheObjects();
		ActionManager manager = new ActionManager();
		manager.getAreasContainingAnySku().add(areas.get(0));
		manager.getAreasContainingAnySku().add(areas.get(1));
		manager.getAreasContainingAnySku().add(areas.get(2));
		manager.computeAreaHavingMinQty("SKU3");
		assertEquals(manager.getAreasWithMinQty().size(), 1);
		assertEquals(manager.getAreasWithMinQty().get(0).getAreaId(), new BigInteger("2"));
	}

	
}
