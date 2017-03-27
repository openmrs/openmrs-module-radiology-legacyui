/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.radiology.legacyui.order.web;

import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.OrderService;
import org.openmrs.module.radiology.dicom.DicomWebViewer;
import org.openmrs.module.radiology.dicom.code.PerformedProcedureStepStatus;
import org.openmrs.module.radiology.legacyui.RadiologyProperties;
import org.openmrs.module.radiology.legacyui.order.web.DiscontinuationOrderRequest;
import org.openmrs.module.radiology.legacyui.order.web.DiscontinuationOrderRequestValidator;
import org.openmrs.module.radiology.legacyui.order.web.RadiologyOrderFormController;
import org.openmrs.module.radiology.legacyui.test.RadiologyTestData;
import org.openmrs.module.radiology.order.RadiologyOrder;
import org.openmrs.module.radiology.order.RadiologyOrderService;
import org.openmrs.module.radiology.order.RadiologyOrderValidator;
import org.openmrs.module.radiology.report.RadiologyReport;
import org.openmrs.module.radiology.report.RadiologyReportService;
import org.openmrs.module.radiology.report.RadiologyReportStatus;
import org.openmrs.test.BaseContextMockTest;
import org.openmrs.web.WebConstants;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

/**
 * Tests {@link RadiologyOrderFormController}
 */
public class RadiologyOrderFormControllerTest extends BaseContextMockTest {
    
    
    @Mock
    private OrderService orderService;
    
    @Mock
    private RadiologyOrderService radiologyOrderService;
    
    @Mock
    private RadiologyReportService radiologyReportService;
    
    @Mock
    private RadiologyProperties radiologyProperties;
    
    @Mock
    private DicomWebViewer dicomWebViewer;
    
    @Mock
    RadiologyOrderValidator radiologyOrderValidator;
    
    @Mock
    DiscontinuationOrderRequestValidator discontinuationOrderRequestValidator;
    
    @InjectMocks
    private RadiologyOrderFormController radiologyOrderFormController = new RadiologyOrderFormController();
    
    private Method radiologyReportNeedsToBeCreatedMethod = null;
    
    @Before
    public void setUp() throws Exception {
        when(radiologyProperties.getRadiologyTestOrderType()).thenReturn(RadiologyTestData.getMockRadiologyOrderType());
        
        radiologyReportNeedsToBeCreatedMethod =
                RadiologyOrderFormController.class.getDeclaredMethod("radiologyReportNeedsToBeCreated",
                    new Class[] { org.springframework.web.servlet.ModelAndView.class, org.openmrs.Order.class });
        radiologyReportNeedsToBeCreatedMethod.setAccessible(true);
    }
    
    @Test
    public void shouldPopulateModelAndViewWithNewRadiologyOrder() throws Exception {
        
        ModelAndView modelAndView = radiologyOrderFormController.getRadiologyOrderFormWithNewRadiologyOrder();
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), is(RadiologyOrderFormController.RADIOLOGY_ORDER_CREATION_FORM_VIEW));
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyOrder"));
        RadiologyOrder order = (RadiologyOrder) modelAndView.getModelMap()
                .get("radiologyOrder");
        assertNull(order.getOrderId());
        
        assertNotNull(order.getStudy());
        assertNull(order.getStudy()
                .getStudyId());
        
        assertNull(order.getOrderer());
    }
    
    @Test
    public void
            getRadiologyOrderFormWithNewRadiologyOrderAndPrefilledPatient_shouldPopulateModelAndViewWithNewRadiologyOrderPrefilledWithGivenPatient()
                    
                    throws Exception {
        
        // given
        Patient mockPatient = RadiologyTestData.getMockPatient1();
        
        ModelAndView modelAndView =
                radiologyOrderFormController.getRadiologyOrderFormWithNewRadiologyOrderAndPrefilledPatient(mockPatient);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), is(RadiologyOrderFormController.RADIOLOGY_ORDER_CREATION_FORM_VIEW));
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyOrder"));
        RadiologyOrder order = (RadiologyOrder) modelAndView.getModelMap()
                .get("radiologyOrder");
        assertNull(order.getOrderId());
        
        assertNotNull(order.getStudy());
        assertNull(order.getStudy()
                .getStudyId());
        
        assertNotNull(order.getPatient());
        assertThat(order.getPatient(), is(mockPatient));
        
        assertThat(modelAndView.getModelMap(), hasKey("patientId"));
        Integer patientId = (Integer) modelAndView.getModelMap()
                .get("patientId");
        assertThat(patientId, is(mockPatient.getPatientId()));
    }
    
    @Test
    public void
            getRadiologyOrderFormWithExistingRadiologyOrder_shouldPopulateModelAndViewWithExistingRadiologyOrderIfGivenOrderIdMatchesARadiologyOrderAndNoDicomViewerUrlIfOrderIsNotCompleted()
                    throws Exception {
        
        // given
        RadiologyOrder mockRadiologyOrderInProgress = RadiologyTestData.getMockRadiologyOrder1();
        mockRadiologyOrderInProgress.getStudy()
                .setPerformedStatus(PerformedProcedureStepStatus.IN_PROGRESS);
        
        ModelAndView modelAndView =
                radiologyOrderFormController.getRadiologyOrderFormWithExistingRadiologyOrder(mockRadiologyOrderInProgress);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), is(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW));
        
        assertThat(modelAndView.getModelMap(), hasKey("order"));
        RadiologyOrder order = (RadiologyOrder) modelAndView.getModelMap()
                .get("order");
        assertThat(order, is(mockRadiologyOrderInProgress));
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyOrder"));
        RadiologyOrder radiologyOrder = (RadiologyOrder) modelAndView.getModelMap()
                .get("radiologyOrder");
        assertThat(radiologyOrder, is(mockRadiologyOrderInProgress));
        
        assertThat(modelAndView.getModelMap(), not(hasKey("dicomViewerUrl")));
    }
    
    @Test
    public void
            getRadiologyOrderFormWithExistingRadiologyOrder_shouldPopulateModelAndViewWithExistingRadiologyOrderIfGivenOrderIdMatchesARadiologyOrderAndDicomViewerUrlIfOrderCompleted()
                    throws Exception {
        
        // given
        RadiologyOrder mockCompletedRadiologyOrder = RadiologyTestData.getMockRadiologyOrder1();
        mockCompletedRadiologyOrder.getStudy()
                .setPerformedStatus(PerformedProcedureStepStatus.COMPLETED);
        
        when(dicomWebViewer.getDicomViewerUrl(mockCompletedRadiologyOrder.getStudy()))
                .thenReturn("http://localhost:8081/weasis-pacs-connector/viewer?studyUID=1.2.826.0.1.3680043.8.2186.1.1");
        
        ModelAndView modelAndView =
                radiologyOrderFormController.getRadiologyOrderFormWithExistingRadiologyOrder(mockCompletedRadiologyOrder);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), is(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW));
        
        assertThat(modelAndView.getModelMap(), hasKey("order"));
        RadiologyOrder order = (RadiologyOrder) modelAndView.getModelMap()
                .get("order");
        assertThat(order, is(mockCompletedRadiologyOrder));
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyOrder"));
        RadiologyOrder radiologyOrder = (RadiologyOrder) modelAndView.getModelMap()
                .get("radiologyOrder");
        assertThat(radiologyOrder, is(mockCompletedRadiologyOrder));
        
        assertThat(modelAndView.getModelMap(), hasKey("dicomViewerUrl"));
        String dicomViewerUrl = (String) modelAndView.getModelMap()
                .get("dicomViewerUrl");
        assertThat(dicomViewerUrl,
            is("http://localhost:8081/weasis-pacs-connector/viewer?studyUID=1.2.826.0.1.3680043.8.2186.1.1"));
    }
    
    @Test
    public void
            getRadiologyOrderFormWithExistingRadiologyOrder_shouldPopulateModelAndViewWithExistingOrderIfGivenOrderIdOnlyMatchesAnOrderAndNotARadiologyOrder()
                    throws Exception {
        
        // given
        RadiologyOrder mockRadiologyOrderToDiscontinue = RadiologyTestData.getMockRadiologyOrder1();
        String discontinueReason = "Wrong Procedure";
        
        Order mockDiscontinuationOrder = new Order();
        mockDiscontinuationOrder.setOrderId(2);
        mockDiscontinuationOrder.setAction(Order.Action.DISCONTINUE);
        mockDiscontinuationOrder.setOrderer(mockRadiologyOrderToDiscontinue.getOrderer());
        mockDiscontinuationOrder.setOrderReasonNonCoded(discontinueReason);
        mockDiscontinuationOrder.setPreviousOrder(mockRadiologyOrderToDiscontinue);
        
        ModelAndView modelAndView =
                radiologyOrderFormController.getRadiologyOrderFormWithExistingRadiologyOrder(mockDiscontinuationOrder);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), is(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW));
        
        assertThat(modelAndView.getModelMap(), hasKey("order"));
        Order order = (Order) modelAndView.getModelMap()
                .get("order");
        assertThat(order, is(mockDiscontinuationOrder));
        
        assertThat(modelAndView.getModelMap(), not(hasKey("radiologyOrder")));
        
        assertThat(modelAndView.getModelMap(), not(hasKey("dicomViewerUrl")));
    }
    
    @Test
    public void
            saveRadiologyOrder_shouldSaveGivenRadiologyOrderIfValidAndSetHttpSessionAttributeOpenmrsMessageToOrderSavedAndRedirectToNewRadiologyOrder()
                    throws Exception {
        
        // given
        RadiologyOrder mockRadiologyOrder = RadiologyTestData.getMockRadiologyOrder1();
        
        when(radiologyOrderService.placeRadiologyOrder(mockRadiologyOrder)).thenReturn(mockRadiologyOrder);
        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addParameter("saveOrder", "saveOrder");
        MockHttpSession mockSession = new MockHttpSession();
        mockRequest.setSession(mockSession);
        
        BindingResult orderErrors = mock(BindingResult.class);
        when(orderErrors.hasErrors()).thenReturn(false);
        
        ModelAndView modelAndView =
                radiologyOrderFormController.saveRadiologyOrder(mockRequest, mockRadiologyOrder, orderErrors);
        
        verify(radiologyOrderService, times(1)).placeRadiologyOrder(mockRadiologyOrder);
        verifyNoMoreInteractions(radiologyOrderService);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(),
            is("redirect:/module/radiology/radiologyOrder.form?orderId=" + mockRadiologyOrder.getOrderId()));
        assertThat((String) mockSession.getAttribute(WebConstants.OPENMRS_MSG_ATTR), is("Order.saved"));
    }
    
    @Test
    public void shouldNotSaveGivenRadiologyOrderIfItIsNotValidAndNotRedirect() throws Exception {
        
        // given
        RadiologyOrder mockRadiologyOrder = RadiologyTestData.getMockRadiologyOrder1();
        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addParameter("saveOrder", "saveOrder");
        MockHttpSession mockSession = new MockHttpSession();
        mockRequest.setSession(mockSession);
        
        BindingResult orderErrors = mock(BindingResult.class);
        when(orderErrors.hasErrors()).thenReturn(true);
        
        ModelAndView modelAndView =
                radiologyOrderFormController.saveRadiologyOrder(mockRequest, mockRadiologyOrder, orderErrors);
        
        verifyZeroInteractions(radiologyOrderService);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), is(RadiologyOrderFormController.RADIOLOGY_ORDER_CREATION_FORM_VIEW));
        
        assertThat(modelAndView.getModelMap(), hasKey("order"));
        Order order = (Order) modelAndView.getModelMap()
                .get("order");
        assertThat(order, is(mockRadiologyOrder));
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyOrder"));
        RadiologyOrder radiologyOrder = (RadiologyOrder) modelAndView.getModelMap()
                .get("radiologyOrder");
        assertThat(radiologyOrder, is(mockRadiologyOrder));
    }
    
    @Test
    public void
            saveRadiologyOrder_shouldNotRedirectAndSetSessionAttributeWithOpenmrsErrorIfApiExceptionIsThrownByPlaceRadiology()
                    throws Exception {
        
        // given
        RadiologyOrder mockRadiologyOrder = RadiologyTestData.getMockRadiologyOrder1();
        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addParameter("saveOrder", "saveOrder");
        MockHttpSession mockSession = new MockHttpSession();
        mockRequest.setSession(mockSession);
        
        BindingResult orderErrors = mock(BindingResult.class);
        when(orderErrors.hasErrors()).thenReturn(false);
        
        when(radiologyOrderService.placeRadiologyOrder(mockRadiologyOrder))
                .thenThrow(new APIException("Order.cannot.edit.existing"));
        
        ModelAndView modelAndView =
                radiologyOrderFormController.saveRadiologyOrder(mockRequest, mockRadiologyOrder, orderErrors);
        
        verify(radiologyOrderService, times(1)).placeRadiologyOrder(mockRadiologyOrder);
        verifyNoMoreInteractions(radiologyOrderService);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), is(RadiologyOrderFormController.RADIOLOGY_ORDER_CREATION_FORM_VIEW));
        
        assertThat(modelAndView.getModelMap(), hasKey("order"));
        Order order = (Order) modelAndView.getModelMap()
                .get("order");
        assertThat(order, is(mockRadiologyOrder));
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyOrder"));
        RadiologyOrder radiologyOrder = (RadiologyOrder) modelAndView.getModelMap()
                .get("radiologyOrder");
        assertThat(radiologyOrder, is(mockRadiologyOrder));
        
        assertThat((String) mockSession.getAttribute(WebConstants.OPENMRS_ERROR_ATTR), is("Order.cannot.edit.existing"));
    }
    
    @Test
    public void shouldDiscontinueNonDiscontinuedRadiologyOrderAndRedirectToDiscontinuationOrder() throws Exception {
        
        // given
        RadiologyOrder mockRadiologyOrderToDiscontinue = RadiologyTestData.getMockRadiologyOrder1();
        
        DiscontinuationOrderRequest discontinuationOrderRequest = new DiscontinuationOrderRequest();
        discontinuationOrderRequest.setOrderer(mockRadiologyOrderToDiscontinue.getOrderer());
        discontinuationOrderRequest.setReasonNonCoded("Wrong Procedure");
        
        Order mockDiscontinuationOrder = new Order();
        mockDiscontinuationOrder.setOrderId(2);
        mockDiscontinuationOrder.setAction(Order.Action.DISCONTINUE);
        mockDiscontinuationOrder.setOrderer(discontinuationOrderRequest.getOrderer());
        mockDiscontinuationOrder.setOrderReasonNonCoded(discontinuationOrderRequest.getReasonNonCoded());
        mockDiscontinuationOrder.setPreviousOrder(mockRadiologyOrderToDiscontinue);
        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addParameter("discontinueOrder", "discontinueOrder");
        MockHttpSession mockSession = new MockHttpSession();
        mockRequest.setSession(mockSession);
        
        when(radiologyOrderService.discontinueRadiologyOrder(mockRadiologyOrderToDiscontinue,
            mockDiscontinuationOrder.getOrderer(), mockDiscontinuationOrder.getOrderReasonNonCoded()))
                    .thenReturn(mockDiscontinuationOrder);
        
        BindingResult resultDiscontinueOrderRequest = mock(BindingResult.class);
        assertThat(mockRadiologyOrderToDiscontinue.getAction(), is(Order.Action.NEW));
        ModelAndView modelAndView = radiologyOrderFormController.discontinueRadiologyOrder(mockRequest,
            mockRadiologyOrderToDiscontinue, discontinuationOrderRequest, resultDiscontinueOrderRequest);
        
        verify(radiologyOrderService, times(1)).discontinueRadiologyOrder(mockRadiologyOrderToDiscontinue,
            discontinuationOrderRequest.getOrderer(), discontinuationOrderRequest.getReasonNonCoded());
        verifyNoMoreInteractions(radiologyOrderService);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(),
            is("redirect:/module/radiology/radiologyOrder.form?orderId=" + mockDiscontinuationOrder.getOrderId()));
        assertThat((String) mockSession.getAttribute(WebConstants.OPENMRS_MSG_ATTR), is("Order.discontinuedSuccessfully"));
    }
    
    @Test
    public void
            discontinueRadiologyOrder_shouldNotDiscontinueGivenRadiologyOrderAndNotRedirectIfDiscontinuationOrderRequestIsNotValid()
                    throws Exception {
        
        // given
        RadiologyOrder mockRadiologyOrderToDiscontinue = RadiologyTestData.getMockRadiologyOrder1();
        
        DiscontinuationOrderRequest discontinuationOrderRequest = new DiscontinuationOrderRequest();
        discontinuationOrderRequest.setOrderer(mockRadiologyOrderToDiscontinue.getOrderer());
        discontinuationOrderRequest.setReasonNonCoded("");
        
        Order mockDiscontinuationOrder = new Order();
        mockDiscontinuationOrder.setOrderId(2);
        mockDiscontinuationOrder.setAction(Order.Action.DISCONTINUE);
        mockDiscontinuationOrder.setOrderer(discontinuationOrderRequest.getOrderer());
        mockDiscontinuationOrder.setOrderReasonNonCoded(discontinuationOrderRequest.getReasonNonCoded());
        mockDiscontinuationOrder.setPreviousOrder(mockRadiologyOrderToDiscontinue);
        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addParameter("discontinueOrder", "discontinueOrder");
        MockHttpSession mockSession = new MockHttpSession();
        mockRequest.setSession(mockSession);
        
        when(radiologyOrderService.discontinueRadiologyOrder(mockRadiologyOrderToDiscontinue,
            mockDiscontinuationOrder.getOrderer(), mockDiscontinuationOrder.getOrderReasonNonCoded()))
                    .thenReturn(mockDiscontinuationOrder);
        
        BindingResult resultDiscontinueOrderRequest = mock(BindingResult.class);
        when(resultDiscontinueOrderRequest.hasErrors()).thenReturn(true);
        
        assertThat(mockRadiologyOrderToDiscontinue.getAction(), is(Order.Action.NEW));
        ModelAndView modelAndView = radiologyOrderFormController.discontinueRadiologyOrder(mockRequest,
            mockRadiologyOrderToDiscontinue, discontinuationOrderRequest, resultDiscontinueOrderRequest);
        
        verifyZeroInteractions(radiologyOrderService);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), is(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW));
        
        assertThat(modelAndView.getModelMap(), hasKey("order"));
        Order order = (Order) modelAndView.getModelMap()
                .get("order");
        assertThat(order, is(mockRadiologyOrderToDiscontinue));
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyOrder"));
        RadiologyOrder radiologyOrder = (RadiologyOrder) modelAndView.getModelMap()
                .get("radiologyOrder");
        assertThat(radiologyOrder, is(mockRadiologyOrderToDiscontinue));
    }
    
    @Test
    public void
            discontinueRadiologyOrder_shouldNotRedirectAndSetSessionAttributeWithOpenmrsErrorIfApiExceptionIsThrownByDiscontinueRadiologyOrder()
                    throws Exception {
        
        // given
        RadiologyOrder mockRadiologyOrderToDiscontinue = RadiologyTestData.getMockRadiologyOrder1();
        
        DiscontinuationOrderRequest discontinuationOrderRequest = new DiscontinuationOrderRequest();
        discontinuationOrderRequest.setOrderer(mockRadiologyOrderToDiscontinue.getOrderer());
        discontinuationOrderRequest.setReasonNonCoded("some");
        
        Order mockDiscontinuationOrder = new Order();
        mockDiscontinuationOrder.setOrderId(2);
        mockDiscontinuationOrder.setAction(Order.Action.DISCONTINUE);
        mockDiscontinuationOrder.setOrderer(discontinuationOrderRequest.getOrderer());
        mockDiscontinuationOrder.setOrderReasonNonCoded(discontinuationOrderRequest.getReasonNonCoded());
        mockDiscontinuationOrder.setPreviousOrder(mockRadiologyOrderToDiscontinue);
        
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addParameter("discontinueOrder", "discontinueOrder");
        MockHttpSession mockSession = new MockHttpSession();
        mockRequest.setSession(mockSession);
        
        when(radiologyOrderService.discontinueRadiologyOrder(mockRadiologyOrderToDiscontinue,
            mockDiscontinuationOrder.getOrderer(), mockDiscontinuationOrder.getOrderReasonNonCoded()))
                    .thenThrow(new APIException("Cannot discontinue an order that is already stopped, expired or voided"));
        
        BindingResult resultDiscontinueOrderRequest = mock(BindingResult.class);
        
        assertThat(mockRadiologyOrderToDiscontinue.getAction(), is(Order.Action.NEW));
        ModelAndView modelAndView = radiologyOrderFormController.discontinueRadiologyOrder(mockRequest,
            mockRadiologyOrderToDiscontinue, discontinuationOrderRequest, resultDiscontinueOrderRequest);
        
        verify(radiologyOrderService, times(1)).discontinueRadiologyOrder(mockRadiologyOrderToDiscontinue,
            discontinuationOrderRequest.getOrderer(), discontinuationOrderRequest.getReasonNonCoded());
        verifyNoMoreInteractions(radiologyOrderService);
        
        assertNotNull(modelAndView);
        assertThat(modelAndView.getViewName(), is(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW));
        
        assertThat(modelAndView.getModelMap(), hasKey("order"));
        Order order = (Order) modelAndView.getModelMap()
                .get("order");
        assertThat(order, is(mockRadiologyOrderToDiscontinue));
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyOrder"));
        RadiologyOrder radiologyOrder = (RadiologyOrder) modelAndView.getModelMap()
                .get("radiologyOrder");
        assertThat(radiologyOrder, is(mockRadiologyOrderToDiscontinue));
        
        assertThat((String) mockSession.getAttribute(WebConstants.OPENMRS_ERROR_ATTR),
            is("Cannot discontinue an order that is already stopped, expired or voided"));
    }
    
    @Test
    public void shouldReturnFalseIfOrderIsNotARadiologyOrder() throws Exception {
        
        // given
        ModelAndView modelAndView = new ModelAndView(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW);
        
        RadiologyOrder mockRadiologyOrderToDiscontinue = RadiologyTestData.getMockRadiologyOrder1();
        String discontinueReason = "Wrong Procedure";
        
        Order mockDiscontinuationOrder = new Order();
        mockDiscontinuationOrder.setOrderId(2);
        mockDiscontinuationOrder.setAction(Order.Action.DISCONTINUE);
        mockDiscontinuationOrder.setOrderer(mockRadiologyOrderToDiscontinue.getOrderer());
        mockDiscontinuationOrder.setOrderReasonNonCoded(discontinueReason);
        mockDiscontinuationOrder.setPreviousOrder(mockRadiologyOrderToDiscontinue);
        
        final boolean result = (Boolean) radiologyReportNeedsToBeCreatedMethod.invoke(radiologyOrderFormController,
            new Object[] { modelAndView, mockDiscontinuationOrder });
        assertFalse(result);
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyReportNeedsToBeCreated"));
        assertFalse((Boolean) modelAndView.getModelMap()
                .get("radiologyReportNeedsToBeCreated"));
    }
    
    /**
     * @see RadiologyOrderFormController#radiologyReportNeedsToBeCreated(ModelAndView,Order)
     */
    @Test
    public void shouldReturnFalseIfRadiologyOrderIsNotCompleted() throws Exception {
        
        // given
        ModelAndView modelAndView = new ModelAndView(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW);
        
        RadiologyOrder incompleteRadiologyOrder = RadiologyTestData.getMockRadiologyOrder1();
        incompleteRadiologyOrder.getStudy()
                .setPerformedStatus(PerformedProcedureStepStatus.IN_PROGRESS);
        
        final boolean result = (Boolean) radiologyReportNeedsToBeCreatedMethod.invoke(radiologyOrderFormController,
            new Object[] { modelAndView, incompleteRadiologyOrder });
        assertFalse(result);
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyReportNeedsToBeCreated"));
        assertFalse((Boolean) modelAndView.getModelMap()
                .get("radiologyReportNeedsToBeCreated"));
    }
    
    @Test
    public void shouldReturnFalseIfRadiologyOrderIsCompletedButHasAClaimedReport() throws Exception {
        
        // given
        ModelAndView modelAndView = new ModelAndView(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW);
        
        RadiologyReport claimedReport = RadiologyTestData.getMockRadiologyReport1();
        claimedReport.setStatus(RadiologyReportStatus.DRAFT);
        
        RadiologyOrder completedRadiologyOrderWithClaimedReport = claimedReport.getRadiologyOrder();
        completedRadiologyOrderWithClaimedReport.getStudy()
                .setPerformedStatus(PerformedProcedureStepStatus.COMPLETED);
        
        when(radiologyReportService.getActiveRadiologyReportByRadiologyOrder(completedRadiologyOrderWithClaimedReport))
                .thenReturn(claimedReport);
        
        final boolean result = (Boolean) radiologyReportNeedsToBeCreatedMethod.invoke(radiologyOrderFormController,
            new Object[] { modelAndView, completedRadiologyOrderWithClaimedReport });
        assertFalse(result);
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyReportNeedsToBeCreated"));
        assertFalse((Boolean) modelAndView.getModelMap()
                .get("radiologyReportNeedsToBeCreated"));
    }
    
    @Test
    public void shouldReturnFalseIfRadiologyOrderIsCompletedButHasACompletedReport() throws Exception {
        
        // given
        ModelAndView modelAndView = new ModelAndView(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW);
        
        RadiologyReport completedReport = RadiologyTestData.getMockRadiologyReport1();
        completedReport.setStatus(RadiologyReportStatus.COMPLETED);
        
        RadiologyOrder completedRadiologyOrderWithCompletedReport = completedReport.getRadiologyOrder();
        completedRadiologyOrderWithCompletedReport.getStudy()
                .setPerformedStatus(PerformedProcedureStepStatus.COMPLETED);
        
        when(radiologyReportService.getActiveRadiologyReportByRadiologyOrder(completedRadiologyOrderWithCompletedReport))
                .thenReturn(completedReport);
        
        final boolean result = (Boolean) radiologyReportNeedsToBeCreatedMethod.invoke(radiologyOrderFormController,
            new Object[] { modelAndView, completedRadiologyOrderWithCompletedReport });
        assertFalse(result);
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyReportNeedsToBeCreated"));
        assertFalse((Boolean) modelAndView.getModelMap()
                .get("radiologyReportNeedsToBeCreated"));
    }
    
    @Test
    public void shouldReturnTrueIfRadiologyOrderIsCompletedAndHasNoClaimedReport() throws Exception {
        
        // given
        ModelAndView modelAndView = new ModelAndView(RadiologyOrderFormController.RADIOLOGY_ORDER_FORM_VIEW);
        
        RadiologyOrder completedRadiologyOrderWithNoClaimedReport = RadiologyTestData.getMockRadiologyOrder1();
        completedRadiologyOrderWithNoClaimedReport.getStudy()
                .setPerformedStatus(PerformedProcedureStepStatus.COMPLETED);
        
        when(radiologyReportService.getActiveRadiologyReportByRadiologyOrder(completedRadiologyOrderWithNoClaimedReport))
                .thenReturn(null);
        
        final boolean result = (Boolean) radiologyReportNeedsToBeCreatedMethod.invoke(radiologyOrderFormController,
            new Object[] { modelAndView, completedRadiologyOrderWithNoClaimedReport });
        assertTrue(result);
        
        assertThat(modelAndView.getModelMap(), hasKey("radiologyReportNeedsToBeCreated"));
        assertTrue((Boolean) modelAndView.getModelMap()
                .get("radiologyReportNeedsToBeCreated"));
    }
}
