package org.plantuml.idea.adapter.rendering;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ObjectUtils;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.Log;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.DiagramDescription;
import net.sourceforge.plantuml.preproc.FileWithSuffix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plantuml.idea.plantuml.PlantUml;
import org.plantuml.idea.rendering.ImageItem;
import org.plantuml.idea.rendering.RenderRequest;
import org.plantuml.idea.rendering.RenderingCancelledException;
import org.plantuml.idea.rendering.RenderingType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.plantuml.idea.adapter.rendering.PlantUmlRendererUtil.checkCancel;
import static org.plantuml.idea.adapter.rendering.PlantUmlRendererUtil.newSourceStringReader;


public class DiagramFactory {
    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(DiagramFactory.class);
    private final List<MyBlock> myBlocks;
    private final int totalPages;

    public DiagramFactory(List<MyBlock> myBlocks, int totalPages) {
        this.myBlocks = myBlocks;
        this.totalPages = totalPages;
    }

    public static DiagramFactory create(RenderRequest renderRequest, String documentSource) {
        SourceStringReader reader = newSourceStringReader(documentSource, renderRequest.isUseSettings(), renderRequest.getSourceFile());
        return create(reader, renderRequest);
    }

    public static DiagramFactory create(SourceStringReader reader, RenderRequest renderRequest) {
        long start1 = System.currentTimeMillis();
        int totalPages = 0;
        List<BlockUml> blocks = reader.getBlocks();
        List<MyBlock> myBlocks = new ArrayList<>();

        if (blocks.size() > 1) {
            LOG.debug("more than 1 block ", blocks);
            //happens when the source is incorrectly extracted and contains multiple diagramFactory
        }

        for (BlockUml blockUml : blocks) {
            checkCancel();
            long start = System.currentTimeMillis();

            MyBlock myBlockInfo = new MyBlock(blockUml);
            if (renderRequest != null) {
                myBlockInfo.zoomDiagram(renderRequest.getFormat(), renderRequest.getZoom());
            }
            myBlocks.add(myBlockInfo);
            totalPages = totalPages + myBlockInfo.getNbImages();
            LOG.debug("myBlockInfo done in  ", System.currentTimeMillis() - start, " ms");

            break;
        }
        DiagramFactory diagramFactory = new DiagramFactory(myBlocks, totalPages);
        LOG.debug("create done in ", System.currentTimeMillis() - start1, "ms");
        return diagramFactory;
    }


    public List<MyBlock> getBlockInfos() {
        return myBlocks;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public String getTitle(int numImage) {
        for (MyBlock myBlock : myBlocks) {
            final int nbInSystem = myBlock.getNbImages();
            if (numImage < nbInSystem) {
                return myBlock.getTitles().getTitle(numImage);
            }
            numImage = numImage - nbInSystem;
        }

        Log.error("numImage is too big = " + numImage);
        return null;
    }

    public String getFilename(int numImage) {
        for (MyBlock myBlock : myBlocks) {
            final int nbInSystem = myBlock.getNbImages();
            if (numImage < nbInSystem) {
                return myBlock.getTitles().getTitle(numImage);
            }
            numImage = numImage - nbInSystem;
        }

        Log.error("numImage is too big = " + numImage);
        return null;
    }

    public DiagramDescription outputImage(OutputStream imageStream, int numImage, FileFormatOption formatOption) {
        try {
            for (MyBlock myBlock : myBlocks) {
                final int nbInSystem = myBlock.getNbImages();
                if (numImage < nbInSystem) {
                    myBlock.getDiagram().exportDiagram(imageStream, numImage, formatOption);
                    return myBlock.getDiagram().getDescription();
                }
                numImage = numImage - nbInSystem;
            }
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new RenderingCancelledException(e);
        }
        Log.error("numImage is too big = " + numImage);
        return null;
    }

    protected byte[] generateSvg(int i) {
        long start = System.currentTimeMillis();
        ByteArrayOutputStream svgStream = new ByteArrayOutputStream();
        outputImage(svgStream, i, PlantUmlNormalRenderer.SVG);
        byte[] svgBytes = svgStream.toByteArray();
        LOG.debug("generated ", PlantUmlNormalRenderer.SVG.getFileFormat(), " for page ", i, " in ", System.currentTimeMillis() - start, "ms");
        return svgBytes;
    }

    @NotNull
    protected ImageItem generateImageItem(RenderRequest renderRequest,
                                          String documentSource,
                                          @Nullable String pageSource,
                                          FileFormatOption formatOption,
                                          int page,
                                          int logPage,
                                          RenderingType renderingType) {
        checkCancel();
        long start = System.currentTimeMillis();

        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();

        DiagramDescription diagramDescription;
        diagramDescription = outputImage(imageStream, page, formatOption);

        byte[] bytes = imageStream.toByteArray();

        LOG.debug("generated ", formatOption.getFileFormat(), " for page ", logPage, " in ", System.currentTimeMillis() - start, "ms");

        byte[] svgBytes = new byte[0];
        if (renderRequest.getFormat() == PlantUml.ImageFormat.SVG) {
            svgBytes = bytes;
        } else if (renderRequest.isRenderUrlLinks()) {
            svgBytes = generateSvg(page);
        }


        ObjectUtils.assertNotNull(diagramDescription);
        String description = diagramDescription.getDescription();
        if (description != null && description.contains("entities")) {
            description = "ok";
        }

        return new ImageItem(renderRequest.getBaseDir(), renderRequest.getFormat(), documentSource, pageSource, page, description, bytes, svgBytes, renderingType, getTitle(page), getFilename(page));
    }

    @NotNull
    public LinkedHashMap<File, Long> getIncludedFiles() {
        long start = System.currentTimeMillis();
        LinkedHashMap<File, Long> includedFiles = new LinkedHashMap<>();
        for (MyBlock block : myBlocks) {
            try {
                Set<File> convert = FileWithSuffix.convert(block.getBlockUml().getIncluded());
                ArrayList<File> files = new ArrayList<>(convert);
                files.sort(File::compareTo);
                for (File file : files) {
                    includedFiles.put(file, file.lastModified());
                }
            } catch (FileNotFoundException e) {
                LOG.warn(e);
            }
        }
        LOG.debug("getIncludedFiles ", (System.currentTimeMillis() - start), "ms");
        return includedFiles;
    }
}