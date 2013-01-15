/* Copyright (c) 2012-2013, University of Edinburgh.
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
package uk.ac.ed.ph.qtiworks.services;

import uk.ac.ed.ph.qtiworks.QtiWorksLogicException;
import uk.ac.ed.ph.qtiworks.QtiWorksRuntimeException;
import uk.ac.ed.ph.qtiworks.base.services.QtiWorksSettings;
import uk.ac.ed.ph.qtiworks.domain.RequestTimestampContext;
import uk.ac.ed.ph.qtiworks.domain.entities.Assessment;
import uk.ac.ed.ph.qtiworks.domain.entities.AssessmentPackage;
import uk.ac.ed.ph.qtiworks.domain.entities.CandidateSession;
import uk.ac.ed.ph.qtiworks.domain.entities.Delivery;
import uk.ac.ed.ph.qtiworks.domain.entities.User;
import uk.ac.ed.ph.qtiworks.utils.IoUtilities;

import uk.ac.ed.ph.jqtiplus.internal.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to manage the creation and deletion of filespaces/sandboxes
 * for storing things like uploaded {@link AssessmentPackage}s and submitted files
 *
 * @author David McKain
 */
@Service
public final class FilespaceManager {

    private static final Logger logger = LoggerFactory.getLogger(FilespaceManager.class);

    @Resource
    private QtiWorksSettings qtiWorksSettings;

    @Resource
    private RequestTimestampContext requestTimestampContext;

    @Resource
    private EntityGraphService entityGraphService;

    private File filesystemBaseDirectory;

    @PostConstruct
    public void init() {
        final String filesystemBaseString = qtiWorksSettings.getFilesystemBase();
        logger.info("Filesystem base for client data is {}", filesystemBaseString);
        this.filesystemBaseDirectory = new File(filesystemBaseString);
        if (!filesystemBaseDirectory.isDirectory()) {
            throw new QtiWorksRuntimeException("Filesystem base path " + filesystemBaseString + " is not a directory");
        }
    }

    public File createTempFile() {
        final String tmpFolderUri = filesystemBaseDirectory.toURI().toString()
                + "/tmp";
        final File candidateItemSessionFolder = ensureCreateDirectory(tmpFolderUri);
        return new File(candidateItemSessionFolder, createUniqueRequestComponent());
    }

    //-------------------------------------------------

    public File createAssessmentPackageSandbox(final User owner) {
        Assert.notNull(owner, "owner");
        final String filespaceUri = filesystemBaseDirectory.toURI().toString()
                + "/assessments/"
                + owner.getBusinessKey()
                + "/" + createUniqueRequestComponent();
        return ensureCreateDirectory(filespaceUri);
    }

    public boolean deleteAssessmentPackageSandbox(final AssessmentPackage assessmentPackage) {
        Assert.notNull(assessmentPackage, "assessmentPackage");
        if (assessmentPackage.getSandboxPath()==null) {
            throw new IllegalStateException("Built-in AssessmentPackages may not be deleted");
        }
        return recursivelyDeleteDirectory(assessmentPackage.getSandboxPath());
    }

    //-------------------------------------------------

    public File createCandidateUploadFile(final CandidateSession candidateSession) {
        Assert.notNull(candidateSession, "candidateSession");
        final String uploadBaseUri = getCandidateSessionUploadBaseUri(candidateSession);
        final File candidateResponseFolder = ensureCreateDirectory(uploadBaseUri);
        return new File(candidateResponseFolder, createUniqueRequestComponent());
    }

    public boolean deleteCandidateUploads(final Delivery delivery) {
        Assert.notNull(delivery, "delivery");
        return recursivelyDeleteDirectory(getCandidateSessionUploadBaseUri(delivery));
    }

    public boolean deleteCandidateUploads(final CandidateSession candidateSession) {
        Assert.notNull(candidateSession, "candidateSession");
        return recursivelyDeleteDirectory(getCandidateSessionUploadBaseUri(candidateSession));
    }

    private String getCandidateSessionUploadBaseUri(final Delivery delivery) {
        final AssessmentPackage assessmentPackage = entityGraphService.getCurrentAssessmentPackage(delivery);
        final Assessment assessment = assessmentPackage.getAssessment();

        final String folderUri = filesystemBaseDirectory.toURI().toString()
                + "/responses/assessment" + assessment.getId()
                + "/package" + assessmentPackage.getId()
                + "/delivery" + delivery.getId();
        return folderUri;
    }

    private String getCandidateSessionUploadBaseUri(final CandidateSession candidateSession) {
        final User candidate = candidateSession.getCandidate();
        final Delivery delivery = candidateSession.getDelivery();

        final String folderUri = getCandidateSessionUploadBaseUri(delivery)
                + "/" + candidate.getBusinessKey()
                + "/session" + candidateSession.getId();
        return folderUri;
    }

    //-------------------------------------------------

    public File obtainCandidateSessionStateStore(final CandidateSession candidateSession) {
        Assert.notNull(candidateSession, "candidateSession");
        return ensureCreateDirectory(getCandidateSessionStoreUri(candidateSession));
    }

    public boolean deleteCandidateSessionData(final Delivery delivery) {
        Assert.notNull(delivery, "delivery");
        return recursivelyDeleteDirectory(getCandidateSessionStoreBaseUri(delivery));
    }

    public boolean deleteCandidateSessionStore(final CandidateSession candidateSession) {
        Assert.notNull(candidateSession, "candidateSession");
        return recursivelyDeleteDirectory(getCandidateSessionStoreUri(candidateSession));
    }

    private final String getCandidateSessionStoreBaseUri(final Delivery delivery) {
        final AssessmentPackage assessmentPackage = entityGraphService.getCurrentAssessmentPackage(delivery);
        final Assessment assessment = assessmentPackage.getAssessment();
        final String folderBaseUri = filesystemBaseDirectory.toURI().toString()
                + "/sessions/assessment" + assessment.getId()
                + "/package" + assessmentPackage.getId()
                + "/delivery" + delivery.getId();
        return folderBaseUri;
    }

    private final String getCandidateSessionStoreUri(final CandidateSession candidateSession) {
        final User candidate = candidateSession.getCandidate();
        final Delivery delivery = candidateSession.getDelivery();
        final String folderUri = getCandidateSessionStoreBaseUri(delivery)
                + "/" + candidate.getBusinessKey()
                + "/session" + candidateSession.getId();
        return folderUri;
    }

    //-------------------------------------------------

    public void deleteSandbox(final File sandboxDirectory) {
        Assert.notNull(sandboxDirectory, "sandboxDirectory");
        recursivelyDeleteDirectory(sandboxDirectory);
    }

    //-------------------------------------------------

    private final File ensureCreateDirectory(final String fileUri) {
        final File directory;
        try {
            directory = new File(URI.create(fileUri));
            return IoUtilities.ensureDirectoryCreated(directory);
        }
        catch (final RuntimeException e) {
            throw new QtiWorksLogicException("Unexpected failure parsing File URI " + fileUri);
        }
        catch (final IOException e) {
            throw new QtiWorksLogicException("Unexpected IO failure creating directory at URI " + fileUri);
        }
    }

    private final boolean recursivelyDeleteDirectory(final String fileUri) {
        return recursivelyDeleteDirectory(fileUriToFile(fileUri));
    }

    private final boolean recursivelyDeleteDirectory(final File directory) {
        if (directory.exists()) {
            /* Do sanity check */
            if (!directory.isDirectory()) {
                throw new QtiWorksLogicException("Expected " + directory.getAbsolutePath() + " to be a directory");
            }
            try {
                IoUtilities.recursivelyDelete(directory);
            }
            catch (final IOException e) {
                logger.warn("Failed to recursively delete directory at {}", directory.getAbsolutePath(), e);
                return false;
            }
        }
        return true;
    }

    private File fileUriToFile(final String fileUri) {
        try {
            return new File(URI.create(fileUri));
        }
        catch (final RuntimeException e) {
            throw new QtiWorksLogicException("Unexpected failure parsing File URI " + fileUri);
        }
    }

    private String createUniqueRequestComponent() {
        final Date timestamp = requestTimestampContext.getCurrentRequestTimestamp();
        final long threadId = Thread.currentThread().getId();
        return new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(timestamp) + "-" + String.valueOf(threadId);
    }
}
