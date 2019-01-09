/*
 * Copyright goes here
 */

package com.honeywell.intelligrated.wes.inventory.inventory.impl.service;

import com.hon.reds.async.outbound.api.AsyncMessage;
import com.hon.reds.async.outbound.api.MessagePublisher;
import com.hon.reds.commons.data.MessageType;
import com.hon.reds.commons.exception.ExceptionReason;
import com.hon.reds.commons.logging.Logger;
import com.hon.reds.commons.logging.REDSLoggerFactory;
import com.hon.reds.core.context.Context;
import com.hon.reds.core.data.Document;
import com.hon.reds.core.data.RestApiResponse;
import com.hon.reds.persistence.client.query.QueryDTO;
import com.hon.reds.persistence.domain.api.domain.UOMBase;
import com.hon.reds.persistence.domain.embeddable.Quantity;
import com.hon.reds.persistence.domain.metadata.Template;
import com.hon.reds.persistence.domain.model.Header;
import com.hon.reds.persistence.domain.model.ModelList;
import com.hon.reds.persistence.domain.model.Page;
import com.hon.reds.persistence.query.Query;
import com.hon.reds.persistence.support.REDSRuntimeExceptionUtil;
import com.honeywell.intelligrated.wes.container.container.rest.client.BaseContainerLocationConsumerServiceImpl;
import com.honeywell.intelligrated.wes.inventory.inventory.api.domain.Inventory;
import com.honeywell.intelligrated.wes.inventory.inventory.api.domain.ItemBase;
import com.honeywell.intelligrated.wes.inventory.inventory.api.service.AllocationDetailService;
import com.honeywell.intelligrated.wes.inventory.inventory.api.service.InventoryLockCodeService;
import com.honeywell.intelligrated.wes.inventory.inventory.api.service.InventoryMapService;
import com.honeywell.intelligrated.wes.inventory.inventory.api.service.ItemService;
import com.honeywell.intelligrated.wes.inventory.inventory.events.InventoryEvent;
import com.honeywell.intelligrated.wes.inventory.inventory.impl.domain.model.InventoryModel;
import com.honeywell.intelligrated.wes.inventory.inventory.impl.ruleaction.AvailableInventory;
import com.honeywell.intelligrated.wes.inventory.inventory.util.Constants;
import com.honeywell.intelligrated.wes.location.location.client.dto.LocationDTO;
import com.honeywell.intelligrated.wes.location.location.client.dto.LocationTypeDTO;
import com.honeywell.intelligrated.wes.location.location.rest.client.LocationConsumerServiceImpl;
import com.honeywell.intelligrated.wes.location.location.rest.client.LocationTypeConsumerService;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryMapServiceImpl extends InventoryMapServiceImplBase
    implements InventoryMapService {
  private static final Logger logger = REDSLoggerFactory.getLogger(InventoryMapServiceImpl.class);

  private static final String MC_COUNTEDQTY = "countedQuantity";
  private static final String MC_LOT = "lot";
  private static final String MC_USERID = "userId";
  private static final String MC_TIMESTAMP = "timestamp";
  private static final String MC_ADJUSTMENT_TYPE = "adjustmentType";
  private static final String MC_ADJUSTMENT_QUANTITY = "adjustmentQuantity";
  private static final String MC_REASON_CODE = "reasonCode";
  @Autowired private BaseContainerLocationConsumerServiceImpl baseContainerLocationConsumerService;

  @Autowired private ItemService itemService;

  @Autowired private MessagePublisher messagePublisher;
  @Autowired private LocationConsumerServiceImpl locationConsumerService;
  @Autowired private LocationTypeConsumerService locationTypeConsumerService;
  @Autowired private InventoryLockCodeService inventoryLockCodeService;
  @Autowired private AvailableInventory availableInventory;

  /**
   * Method implementation to create an inventory based on container lpn or location id
   *
   * @param inventoryDoc - Document to create a container
   * @param template - Template
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public Document createInventoryByLocationOrContainerLpn(
      Document inventoryDoc, Template template) {
    logger.trace(() -> ">> createInventoryByLocationOrContainerLpn()");
    logger.debug(
        () ->
            MessageFormat.format(
                "-- createInventoryByLocationOrContainerLpn() creation of new Inventory Model for user {0} and Organization {1}",
                Context.getContext().getUser(), Context.getContext().getOrganization()));

    inventoryDoc.put(
        Inventory.Fields.BASE_CONTAINER_LOCATION_ID.getCode(),
        fetchBaseContainerLocationIdWithValidation(inventoryDoc));
    inventoryDoc = updateExistingInventoryOnMatchOrCreateInventory(inventoryDoc, template);
    logger.trace(() -> "<< createInventoryByLocationOrContainerLpn()");
    return inventoryDoc;
  }

}
