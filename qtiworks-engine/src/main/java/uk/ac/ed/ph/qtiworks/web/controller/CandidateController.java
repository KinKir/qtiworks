/* Copyright (c) 2012, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.qtiworks.web.controller;

import uk.ac.ed.ph.qtiworks.rendering.Renderer;
import uk.ac.ed.ph.qtiworks.rendering.SerializationMethod;
import uk.ac.ed.ph.qtiworks.samples.AllSampleSets;
import uk.ac.ed.ph.qtiworks.samples.QtiSampleResource;
import uk.ac.ed.ph.qtiworks.samples.QtiSampleSet;
import uk.ac.ed.ph.qtiworks.services.CandidateUploadService;
import uk.ac.ed.ph.qtiworks.web.exception.QtiSampleNotFoundException;

import uk.ac.ed.ph.jqtiplus.JqtiExtensionManager;
import uk.ac.ed.ph.jqtiplus.exception2.QtiLogicException;
import uk.ac.ed.ph.jqtiplus.exception2.RuntimeValidationException;
import uk.ac.ed.ph.jqtiplus.node.ModelRichness;
import uk.ac.ed.ph.jqtiplus.reading.QtiXmlObjectReader;
import uk.ac.ed.ph.jqtiplus.reading.QtiXmlReader;
import uk.ac.ed.ph.jqtiplus.resolution.AssessmentObjectManager;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentItem;
import uk.ac.ed.ph.jqtiplus.running.ItemSessionController;
import uk.ac.ed.ph.jqtiplus.state.ItemSessionState;
import uk.ac.ed.ph.jqtiplus.types.FileResponseData;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.types.ResponseData;
import uk.ac.ed.ph.jqtiplus.types.StringResponseData;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ClassPathResourceLocator;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ResourceLocator;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * First stab at controller for doing candidate (item) sessions
 *
 * @author David McKain
 */
@Controller
public class CandidateController {
    
    private static final Logger logger = LoggerFactory.getLogger(CandidateController.class);
    
    private static final String CURRENT_ITEM = "currentItem";
    private static final String CURRENT_ITEM_SESSION_STATE = "currentItemSessionState";
    
    @Resource
    private JqtiExtensionManager jqtiExtensionManager;
    
    @Resource
    private CandidateUploadService candidateUploadService;
    
    @Resource
    private QtiXmlReader qtiXmlReader;
    
    @Resource
    private Renderer renderer;

    /**
     * Starts a new item session using the sample resource with the given path
     */
    @RequestMapping(value="/newSession/{setIndex}/{itemIndex}", method=RequestMethod.GET)
    public String newSampleItemSession(HttpSession httpSession, @PathVariable int setIndex, @PathVariable int itemIndex) {
        logger.info("newSampleItemSession(setIndex={}, itemIndex={})", setIndex, itemIndex);
        
        final QtiSampleSet[] allSampleSets = AllSampleSets.asArray();
        if (setIndex < 0 || setIndex >= allSampleSets.length) {
            throw new QtiSampleNotFoundException("Could not find sample set with index " + setIndex);
        }
        final QtiSampleSet qtiSampleSet = allSampleSets[setIndex];
        final List<QtiSampleResource> qtiSampleResources = qtiSampleSet.getResources();
        if (itemIndex <0 || itemIndex >= qtiSampleResources.size()) {
            throw new QtiSampleNotFoundException("Could not find sample resource with index " + setIndex + " in set " + qtiSampleSet);
        }
        final QtiSampleResource qtiSampleResource = qtiSampleResources.get(itemIndex);
        logger.info("Starting new session for {}", qtiSampleResource);

        /* Load and resolve item */
        final ResourceLocator sampleResourceLocator = new ClassPathResourceLocator();
        final URI sampleResourceUri = qtiSampleResource.toClassPathUri();
        final QtiXmlObjectReader objectReader = qtiXmlReader.createQtiXmlObjectReader(sampleResourceLocator);
        final AssessmentObjectManager objectManager = new AssessmentObjectManager(objectReader);
        ResolvedAssessmentItem resolvedAssessmentItem = objectManager.resolveAssessmentItem(sampleResourceUri, ModelRichness.FULL_ASSUMED_VALID);
        
        /* Create new item session */
        ItemSessionState itemSessionState = new ItemSessionState();
        
        /* TEMP: Store things in the HTTP session */
        httpSession.setAttribute(CURRENT_ITEM, resolvedAssessmentItem);
        httpSession.setAttribute(CURRENT_ITEM_SESSION_STATE, itemSessionState);

        /* Redirect to session handler */
        return "redirect:/dispatcher/itemSession";
    }
    
    @RequestMapping(value="/itemSession", method={ RequestMethod.GET, RequestMethod.POST })
    @ResponseBody
    public String sampleItemSession(HttpServletRequest request, HttpSession httpSession) {
        /* TEMP: Extract current item & state from HTTP session */
        ResolvedAssessmentItem resolvedAssessmentItem = (ResolvedAssessmentItem) httpSession.getAttribute(CURRENT_ITEM);
        ItemSessionState itemSessionState = (ItemSessionState) httpSession.getAttribute(CURRENT_ITEM_SESSION_STATE);
        
        String pageContent = null;
        try {
            if (!itemSessionState.isInitialized()) {
                /* Session hasn't been initialized yet */
                pageContent = serveFreshItem(resolvedAssessmentItem, itemSessionState);
            }
            else {
                /* Session initialised, so handle responses */
                pageContent = handleResponseSubmission(request, resolvedAssessmentItem, itemSessionState);
            }
        }
        catch (RuntimeValidationException e) {
            throw new QtiLogicException("Unexpected RuntimeValidationException encountered", e);
        }
        return pageContent;
    }
    
    private String serveFreshItem(ResolvedAssessmentItem resolvedAssessmentItem, ItemSessionState itemSessionState)
            throws RuntimeValidationException {
        ItemSessionController itemController = new ItemSessionController(jqtiExtensionManager, resolvedAssessmentItem, itemSessionState);
        itemController.initialize();
        
        Map<String, Object> renderingParameters = createRenderingParameters();
        return renderer.renderFreshStandaloneItem(resolvedAssessmentItem, itemSessionState,
                "/RESOURCES-TODO", renderingParameters, SerializationMethod.HTML5_MATHJAX);
    }
    
    private String handleResponseSubmission(HttpServletRequest request, ResolvedAssessmentItem resolvedAssessmentItem, ItemSessionState itemSessionState)
            throws RuntimeValidationException {
        /* First need to extract responses */
        Map<String, ResponseData> responseMap = new HashMap<String, ResponseData>();
        extractStringResponseData(request, responseMap);
        if (request instanceof MultipartHttpServletRequest) {
            extractFileResponseData((MultipartHttpServletRequest) request, responseMap);
        }
        logger.debug("Extracted responses {}", responseMap);

        /* Bind responses */
        ItemSessionController itemController = new ItemSessionController(jqtiExtensionManager, resolvedAssessmentItem, itemSessionState);
        List<Identifier> invalidResponseIdentifiers = null;
        List<Identifier> badResponseIdentifiers = itemController.bindResponses(responseMap);
        if (badResponseIdentifiers.isEmpty()) {
            logger.debug("Responses bound successfully, so continuing to response validation step");
            invalidResponseIdentifiers = itemController.validateResponses();
            if (invalidResponseIdentifiers.isEmpty()) {
                logger.debug("Responses validated successfully, so invoking response processing");
                itemController.processResponses();
            }
            else {
                logger.debug("Invalid responses submitted to {}, not invoking response processing", invalidResponseIdentifiers);
            }
        }
        else {
            logger.debug("Bad responses submitted to {}, so no response processing invoked", badResponseIdentifiers);
        }
        
        Map<String, Object> renderingParameters = createRenderingParameters();
        return renderer.renderRespondedStandaloneItem(resolvedAssessmentItem, itemSessionState,
                "/RESOURCES-TODO", responseMap, badResponseIdentifiers, invalidResponseIdentifiers,
                renderingParameters, SerializationMethod.HTML5_MATHJAX);
    }
    
    private Map<String, Object> createRenderingParameters() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("showInternalState", Boolean.TRUE);
        result.put("displayTitle", Boolean.TRUE);
        result.put("displayControls", Boolean.TRUE);
        return result;
    }
    
    public void extractFileResponseData(MultipartHttpServletRequest multipartRequest, Map<String, ResponseData> responseMap) {
        @SuppressWarnings("unchecked")
        Set<String> parameterNames = multipartRequest.getParameterMap().keySet();
        for (String name : parameterNames) {
            if (name.startsWith("qwuploadpresented_")) {
                String responseIdentifier = name.substring("qwuploadpresented_".length());
                FileResponseData fileResponseData = null;
                MultipartFile multipartFile = multipartRequest.getFile("qwuploadresponse_" + responseIdentifier);
                if (multipartFile!=null) {
                    fileResponseData = candidateUploadService.importData(multipartFile);
                }
                responseMap.put(responseIdentifier, fileResponseData);
            }
        }
    }
    
    public void extractStringResponseData(HttpServletRequest request, Map<String, ResponseData> responseMap) {
        @SuppressWarnings("unchecked")
        Set<String> parameterNames = request.getParameterMap().keySet();
        for (String name : parameterNames) {
            if (name.startsWith("qwpresented_")) {
                String responseIdentifier = name.substring("qwpresented_".length());
                String[] responseValues = request.getParameterValues("qwresponse_" + responseIdentifier);
                StringResponseData stringResponseData = new StringResponseData(responseValues);
                responseMap.put(responseIdentifier, stringResponseData);
            }
        }
    }
}