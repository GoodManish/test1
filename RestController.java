
@RunWith(MockitoJUnitRunner.class)
public class ContainerMapServiceImplTest {

  @Spy @InjectMocks ContainerMapServiceImpl containerMapServiceImpl;

  @Mock ContainerService containerService;

  @Mock ContainerStatusMapService containerStatusMapService;

  @Mock ContainerPathService containerPathService;

  @Mock List<ContainerPathModel> resultContainerPathList;


  private static final String LOCATION_ALIAS = "LocationAlias";
  private static final String PHYSICAL_DESTINATION = "PhysicalDestination";
  private static final String CONTAINER_LPN = "ContainerLpn";

  @Before
  public void init() {
    Context.createTestContext();
  }

  @Test(expected = REDSRuntimeException.class)
  public void createContainerWithHierarchy_null_document_throwsException() {
    containerMapServiceImpl.createContainerWithHierarchy(any(Document.class), any(Template.class));
  }

}
