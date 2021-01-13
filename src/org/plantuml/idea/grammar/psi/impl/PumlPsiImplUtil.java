package org.plantuml.idea.grammar.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;
import org.plantuml.idea.grammar.PumlItemReference;
import org.plantuml.idea.grammar.psi.PumlElementFactory;
import org.plantuml.idea.grammar.psi.PumlItem;
import org.plantuml.idea.lang.PlantUmlFileType;

import javax.swing.*;

public class PumlPsiImplUtil {
    public static String getName(PumlItem element) {
        return element.getText();
    }

    public static PsiElement getNameIdentifier(PumlItem element) {
        return (PsiElement) element.getNode().getFirstChildNode();
    }

    public static PsiElement setName(PumlItem element, String newName) {
        ASTNode keyNode = element.getNode().getFirstChildNode();
        if (keyNode != null) {
            PumlItem property = PumlElementFactory.createWord(element.getProject(), newName);
            ASTNode newKeyNode = property.getFirstChild().getNode();
            element.getNode().replaceChild(keyNode, newKeyNode);
        }
        return element;
    }


    public static ItemPresentation getPresentation(final PumlItem element) {
        return new ItemPresentation() {
            @Nullable
            @Override
            public String getPresentableText() {
                return element.getText();
            }

            @Nullable
            @Override
            public String getLocationString() {
                long start = System.currentTimeMillis();
                PsiFile containingFile = element.getContainingFile();
                String s = containingFile == null ? null : containingFile.getName();
                System.err.println("getLocationString " + (System.currentTimeMillis() - start) + "ms");
                return s;
            }

            @Override
            public Icon getIcon(boolean unused) {
                return PlantUmlFileType.PLANTUML_ICON;
            }
        };
    }

    public static ItemPresentation getPresentation2(final PumlItem element, Document document) {
        return new ItemPresentation() {
            @Nullable
            @Override
            public String getPresentableText() {
                return element.getText();
            }

            @Nullable
            @Override
            public String getLocationString() {
                if (document == null) {
                    return null;
                }
                long start = System.currentTimeMillis();
                int lineNumber = document.getLineNumber(element.getTextOffset());
                return "line: " + lineNumber;
            }

            @Override
            public Icon getIcon(boolean unused) {
                return PlantUmlFileType.PLANTUML_ICON;
            }
        };
    }

    public static PsiReference getReference(PumlItem element) {
        return new PumlItemReference(element, element.getText());
    }

    @Deprecated
    public static PsiReference[] getReferences(PumlItem element) {
        return new PsiReference[]{getReference(element)};
    }

    public static String toString(PumlItem element) {
        return element.getClass().getSimpleName() + "(" + element.getText() + ")" + element.getTextRange();
    }

}
