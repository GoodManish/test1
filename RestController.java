package com.honeywell.intelligrated.wes.container.container.impl.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hon.reds.commons.exception.REDSRuntimeException;
import com.hon.reds.core.context.Context;
import com.hon.reds.core.data.Document;
import com.hon.reds.core.data.RestApiResponse;
import com.hon.reds.core.data.RestApiResponseFactory;
import com.hon.reds.persistence.client.query.QueryDTO;
import com.hon.reds.persistence.domain.metadata.Template;
import com.hon.reds.persistence.domain.model.ModelList;
import com.hon.reds.persistence.domain.model.Page;
import com.hon.reds.persistence.domain.model.PagingRequest;
import com.hon.reds.persistence.query.Query;
import com.honeywell.intelligrated.wes.container.container.api.domain.BaseContainerLocation;
import com.honeywell.intelligrated.wes.container.container.api.domain.Container;
import com.honeywell.intelligrated.wes.container.container.api.domain.ContainerBase;
import com.honeywell.intelligrated.wes.container.container.api.domain.ContainerCompartmentDtl;
import com.honeywell.intelligrated.wes.container.container.api.domain.ContainerLpn;
import com.honeywell.intelligrated.wes.container.container.api.domain.ContainerLpnBase;
import com.honeywell.intelligrated.wes.container.container.api.domain.ContainerPath;
import com.honeywell.intelligrated.wes.container.container.api.domain.ContainerQuarantineReasonCode;
import com.honeywell.intelligrated.wes.container.container.api.domain.ContainerTypeBase;
import com.honeywell.intelligrated.wes.container.container.api.service.BaseContainerLocationMapService;
import com.honeywell.intelligrated.wes.container.container.api.service.BaseContainerLocationService;
import com.honeywell.intelligrated.wes.container.container.api.service.ContainerMapServiceBase;
import com.honeywell.intelligrated.wes.container.container.api.service.ContainerPathMapService;
import com.honeywell.intelligrated.wes.container.container.api.service.ContainerPathService;
import com.honeywell.intelligrated.wes.container.container.api.service.ContainerService;
import com.honeywell.intelligrated.wes.container.container.api.service.ContainerServiceBase;
import com.honeywell.intelligrated.wes.container.container.api.service.ContainerStatusMapService;
import com.honeywell.intelligrated.wes.container.container.api.service.ContainerTypeServiceBase;
import com.honeywell.intelligrated.wes.container.container.impl.domain.model.BaseContainerLocationModelBuilder;
import com.honeywell.intelligrated.wes.container.container.impl.domain.model.ContainerLpnModel;
import com.honeywell.intelligrated.wes.container.container.impl.domain.model.ContainerModel;
import com.honeywell.intelligrated.wes.container.container.impl.domain.model.ContainerModelBuilder;
import com.honeywell.intelligrated.wes.container.container.impl.domain.model.ContainerModelBuilder.Key;
import com.honeywell.intelligrated.wes.container.container.impl.domain.model.ContainerPathModel;
import com.honeywell.intelligrated.wes.container.container.impl.domain.model.ContainerPathModelBuilder;
import com.honeywell.intelligrated.wes.container.container.impl.domain.model.ContainerTypeModel;
import com.honeywell.intelligrated.wes.container.container.impl.service.business.ConstructWidgetTestData;
import com.honeywell.intelligrated.wes.container.container.service.business.model.ContainerWidgetData;
import com.honeywell.intelligrated.wes.location.location.client.dto.LocationBaseDTO;
import com.honeywell.intelligrated.wes.location.location.client.dto.LocationDTO;
import com.honeywell.intelligrated.wes.location.location.rest.client.LocationConsumerServiceImpl;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

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
