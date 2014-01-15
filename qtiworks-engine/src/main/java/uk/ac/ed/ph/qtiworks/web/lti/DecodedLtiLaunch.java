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
 * This software is derived from (and contains code from) QTITools and MathAssessEngine.
 * QTITools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.qtiworks.web.lti;

import uk.ac.ed.ph.qtiworks.domain.entities.LtiUser;

import uk.ac.ed.ph.jqtiplus.internal.util.DumpMode;
import uk.ac.ed.ph.jqtiplus.internal.util.ObjectDumperOptions;
import uk.ac.ed.ph.jqtiplus.internal.util.ObjectUtilities;

/**
 * Encapsulates the result of decoding an LTI launch request.
 *
 * @see LtiLaunchService#decodeLtiLaunchData(javax.servlet.http.HttpServletRequest, uk.ac.ed.ph.qtiworks.domain.entities.LtiLaunchType)
 *
 * @author David McKain
 */
public final class DecodedLtiLaunch {

    private final LtiLaunchData ltiLaunchData;
    private final int errorCode;
    private final String errorMessage;
    private final LtiUser ltiUser;

    public DecodedLtiLaunch(final LtiLaunchData ltiLaunchData, final int errorCode, final String errorMessage) {
        this(ltiLaunchData, errorCode, errorMessage, null);
    }

    public DecodedLtiLaunch(final LtiLaunchData ltiLaunchData, final LtiUser ltiUser) {
        this(ltiLaunchData, 0, null, ltiUser);
    }

    private DecodedLtiLaunch(final LtiLaunchData ltiLaunchData, final int errorCode, final String errorMessage, final LtiUser ltiUser) {
        this.ltiLaunchData = ltiLaunchData;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.ltiUser = ltiUser;
    }

    @ObjectDumperOptions(DumpMode.DEEP)
    public LtiLaunchData getLtiLaunchData() {
        return ltiLaunchData;
    }

    public boolean isError() {
        return errorCode!=0;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @ObjectDumperOptions(DumpMode.DEEP)
    public LtiUser getLtiUser() {
        return ltiUser;
    }

    @Override
    public String toString() {
        return ObjectUtilities.beanToString(this);
    }
}
