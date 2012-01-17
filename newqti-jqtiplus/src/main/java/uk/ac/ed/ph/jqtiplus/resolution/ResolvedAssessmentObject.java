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
package uk.ac.ed.ph.jqtiplus.resolution;

import uk.ac.ed.ph.jqtiplus.node.AssessmentObject;
import uk.ac.ed.ph.jqtiplus.node.ModelRichness;
import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.shared.VariableDeclaration;
import uk.ac.ed.ph.jqtiplus.node.test.AssessmentTest;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.types.VariableReferenceIdentifier;
import uk.ac.ed.ph.jqtiplus.validation.AssessmentObjectValidator;

import java.io.Serializable;

/**
 * Base class for the result of "resolving" an {@link AssessmentObject}.
 * 
 * @see AssessmentObjectManager
 * @see AssessmentObjectValidator
 * 
 * @author David McKain
 */
public abstract class ResolvedAssessmentObject<E extends AssessmentObject> implements Serializable {

    private static final long serialVersionUID = 7287850555177299535L;

    /** {@link AssessmentObject} lookup */
    protected final RootObjectLookup<E> objectLookup;
    
    protected final ModelRichness modelRichness;

    public ResolvedAssessmentObject(final ModelRichness modelRichness, final RootObjectLookup<E> objectLookup) {
        this.objectLookup = objectLookup;
        this.modelRichness = modelRichness;
    }
    
    public RootObjectLookup<E> getObjectLookup() {
        return objectLookup;
    }
    
    public ModelRichness getModelRichness() {
        return modelRichness;
    }
    
    /**
     * Resolves a declared variable in the current {@link AssessmentObject} having the given {@link Identifier}.
     * 
     * @param variableDeclarationIdentifier
     * @return resulting {@link VariableDeclaration}, which will not be null.
     * @throws VariableResolutionException if the variable be resolved. The Exception will contain specific
     *   details about why this happened.
     */
    public abstract VariableDeclaration resolveVariableReference(Identifier variableDeclarationIdentifier)
            throws VariableResolutionException;
    
    /**
     * Resolves a referenced variable in the current {@link AssessmentItem}, or the current {@link AssessmentTest}
     * and all referenced {@link AssessmentItem}s.
     * 
     * @param variableReferenceIdentifier
     * @return resulting {@link VariableDeclaration}, which will not be null.
     * @throws VariableResolutionException if the variable be resolved. The Exception will contain specific
     *   details about why this happened.
     */
    public abstract VariableDeclaration resolveVariableReference(VariableReferenceIdentifier variableReferenceIdentifier)
            throws VariableResolutionException;
    
}