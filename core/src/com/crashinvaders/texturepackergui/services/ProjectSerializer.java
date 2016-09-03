package com.crashinvaders.texturepackergui.services;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.Array;
import com.crashinvaders.texturepackergui.events.ProjectSerializerEvent;
import com.crashinvaders.texturepackergui.events.ToastNotificationEvent;
import com.crashinvaders.texturepackergui.services.model.PackModel;
import com.crashinvaders.texturepackergui.services.model.ProjectModel;
import com.crashinvaders.texturepackergui.utils.PathUtils;
import com.github.czyzby.autumn.annotation.Component;
import com.github.czyzby.autumn.annotation.Inject;
import com.github.czyzby.autumn.mvc.component.i18n.LocaleService;
import com.github.czyzby.autumn.processor.event.EventDispatcher;

import java.io.File;
import java.io.IOException;

import static com.crashinvaders.texturepackergui.utils.CommonUtils.splitAndTrim;
import static com.crashinvaders.texturepackergui.utils.FileUtils.loadTextFromFile;
import static com.crashinvaders.texturepackergui.utils.FileUtils.saveTextToFile;

@Component
public class ProjectSerializer {

    @Inject EventDispatcher eventDispatcher;
    @Inject LocaleService localeService;

    public void saveProject(ProjectModel project, FileHandle file) {
        //TODO handle errors and notify with event

        String serialized = serializeProject(project, file.parent());
        try {
            saveTextToFile(serialized, file);
        } catch (IOException e) {
            eventDispatcher.postEvent(new ToastNotificationEvent().message(localeService.getI18nBundle()
                    .format("toastProjectSaveError", project.getProjectFile().path())));
            return;
        }

        eventDispatcher.postEvent(new ProjectSerializerEvent(ProjectSerializerEvent.Action.SAVED, file));
    }

    public ProjectModel loadProject(FileHandle file) {
        //TODO handle errors and notify with event

        String serialized;
        try {
            serialized = loadTextFromFile(file);
        } catch (IOException e) {
            eventDispatcher.postEvent(new ToastNotificationEvent().message(localeService.getI18nBundle()
                    .format("toastProjectLoadError", file.path())));
            return null;
        }

        ProjectModel project = deserializeProject(serialized, file.parent());
        project.setProjectFile(file);

        eventDispatcher.postEvent(new ProjectSerializerEvent(ProjectSerializerEvent.Action.LOADED, file));
        return project;
    }

    private String serializeProject(ProjectModel projectModel, FileHandle root) {
        Array<PackModel> packs = projectModel.getPacks();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < packs.size; i++) {
            sb.append(serializePack(packs.get(i), root));

            if (i < packs.size - 1) {
                sb.append("\n\n---\n\n");
            }
        }

        return sb.toString();
    }

    private String serializePack(PackModel pack, FileHandle root) {
        StringBuilder sb = new StringBuilder();

        String filename = pack.getFilename();

        sb.append("name=").append(pack.getName()).append("\n");
        sb.append("filename=").append(filename).append("\n");
        sb.append("input=").append(PathUtils.relativize(pack.getInputDir(), root.file().getPath())).append("\n");
        sb.append("output=").append(PathUtils.relativize(pack.getOutputDir(), root.file().getPath())).append("\n");

        sb.append("\n");

        TexturePacker.Settings settings = pack.getSettings();
        sb.append("alias=").append(settings.alias).append("\n");
        sb.append("alphaThreshold=").append(settings.alphaThreshold).append("\n");
        sb.append("debug=").append(settings.debug).append("\n");
        sb.append("duplicatePadding=").append(settings.duplicatePadding).append("\n");
        sb.append("edgePadding=").append(settings.edgePadding).append("\n");
        sb.append("fast=").append(settings.fast).append("\n");
        sb.append("filterMag=").append(settings.filterMag).append("\n");
        sb.append("filterMin=").append(settings.filterMin).append("\n");
        sb.append("format=").append(settings.format).append("\n");
        sb.append("ignoreBlankImages=").append(settings.ignoreBlankImages).append("\n");
        sb.append("jpegQuality=").append(settings.jpegQuality).append("\n");
        sb.append("maxHeight=").append(settings.maxHeight).append("\n");
        sb.append("maxWidth=").append(settings.maxWidth).append("\n");
        sb.append("minHeight=").append(settings.minHeight).append("\n");
        sb.append("minWidth=").append(settings.minWidth).append("\n");
        sb.append("outputFormat=").append(settings.outputFormat).append("\n");
        sb.append("paddingX=").append(settings.paddingX).append("\n");
        sb.append("paddingY=").append(settings.paddingY).append("\n");
        sb.append("pot=").append(settings.pot).append("\n");
        sb.append("rotation=").append(settings.rotation).append("\n");
        sb.append("stripWhitespaceX=").append(settings.stripWhitespaceX).append("\n");
        sb.append("stripWhitespaceY=").append(settings.stripWhitespaceY).append("\n");
        sb.append("wrapX=").append(settings.wrapX).append("\n");
        sb.append("wrapY=").append(settings.wrapY).append("\n");
        sb.append("premultiplyAlpha=").append(settings.premultiplyAlpha).append("\n");
        sb.append("combineSubdirectories=").append(settings.combineSubdirectories);

        return sb.toString();
    }

    private ProjectModel deserializeProject(String serializedProject, FileHandle root) {
        ProjectModel project = new ProjectModel();

        String[] serializedPacks = serializedProject.split("---");

        for (String serializedPack : serializedPacks) {
            PackModel pack = deserializePack(serializedPack, root);
            project.addPack(pack);
        }
        return project;
    }


    private PackModel deserializePack(String serializedData, FileHandle root) {
        PackModel pack = new PackModel();

        Array<String> lines = splitAndTrim(serializedData);
        for (String line : lines) {
            if (line.startsWith("name=")) pack.setName(PathUtils.trim(line.substring("name=".length())).trim());
            if (line.startsWith("filename=")) pack.setFilename(PathUtils.trim(line.substring("filename=".length())).trim());
            if (line.startsWith("input=")) pack.setInputDir(PathUtils.trim(line.substring("input=".length())).trim());
            if (line.startsWith("output=")) pack.setOutputDir(PathUtils.trim(line.substring("output=".length())).trim());
        }

        try {
            String inputDir = pack.getInputDir();
            if (!inputDir.equals("") && !new File(inputDir).isAbsolute()) {
                pack.setInputDir(new File(root.file(), inputDir).getCanonicalPath());
            }
            String outputDir = pack.getOutputDir();
            if (!outputDir.equals("") && !new File(outputDir).isAbsolute()) {
                pack.setOutputDir(new File(root.file(), outputDir).getCanonicalPath());
            }
        } catch (IOException ex) {
            //TODO show error to user somehow
            System.err.println(ex.getMessage());
            pack.setInputDir("");
            pack.setOutputDir("");
        }

        TexturePacker.Settings settings = pack.getSettings();
        TexturePacker.Settings defaultSettings = new TexturePacker.Settings();

        settings.alias = find(lines, "atlas=", defaultSettings.alias);
        settings.alphaThreshold = find(lines, "alphaThreshold=", defaultSettings.alphaThreshold);
        settings.debug = find(lines, "debug=", defaultSettings.debug);
        settings.duplicatePadding = find(lines, "duplicatePadding=", defaultSettings.duplicatePadding);
        settings.edgePadding = find(lines, "edgePadding=", defaultSettings.edgePadding);
        settings.fast = find(lines, "fast=", defaultSettings.fast);
        settings.filterMag = Texture.TextureFilter.valueOf(find(lines, "filterMag=", defaultSettings.filterMag.toString()));
        settings.filterMin = Texture.TextureFilter.valueOf(find(lines, "filterMin=", defaultSettings.filterMin.toString()));
        settings.format = Pixmap.Format.valueOf(find(lines, "format=", defaultSettings.format.toString()));
        settings.ignoreBlankImages = find(lines, "ignoreBlankImages=", defaultSettings.ignoreBlankImages);
        settings.jpegQuality = find(lines, "jpegQuality=", defaultSettings.jpegQuality);
        settings.maxHeight = find(lines, "maxHeight=", 2048); // defaultSettings.maxHeight value (1024) is outdated and 2048 is recommended
        settings.maxWidth = find(lines, "maxWidth=", 2048); // defaultSettings.maxWidth value (1024) is outdated and 2048 is recommended
        settings.minHeight = find(lines, "minHeight=", defaultSettings.minHeight);
        settings.minWidth = find(lines, "minWidth=", defaultSettings.minWidth);
        settings.outputFormat = find(lines, "outputFormat=", defaultSettings.outputFormat);
        settings.paddingX = find(lines, "paddingX=", defaultSettings.paddingX);
        settings.paddingY = find(lines, "paddingY=", defaultSettings.paddingY);
        settings.pot = find(lines, "pot=", defaultSettings.pot);
        settings.rotation = find(lines, "rotation=", defaultSettings.rotation);
        settings.stripWhitespaceX = find(lines, "stripWhitespaceX=", defaultSettings.stripWhitespaceX);
        settings.stripWhitespaceY = find(lines, "stripWhitespaceY=", defaultSettings.stripWhitespaceY);
        settings.wrapX = Texture.TextureWrap.valueOf(find(lines, "wrapX=", defaultSettings.wrapX.toString()));
        settings.wrapY = Texture.TextureWrap.valueOf(find(lines, "wrapY=", defaultSettings.wrapY.toString()));
        settings.premultiplyAlpha = find(lines, "premultiplyAlpha=", defaultSettings.premultiplyAlpha);
        settings.combineSubdirectories = find(lines, "combineSubdirectories=", defaultSettings.combineSubdirectories);

        return pack;
    }

    private static String find (Array<String> lines, String start, String defaultValue) {
        for (String line : lines) {
            if (line.startsWith(start)) return line.substring(start.length());
        }
        return defaultValue;
    }
    private static boolean find (Array<String> lines, String start, boolean defaultValue) {
        String str = find(lines, start, null);
        if (str != null) return Boolean.parseBoolean(str);
        return defaultValue;
    }
    private static int find (Array<String> lines, String start, int defaultValue) {
        String str = find(lines, start, null);
        if (str != null) return Integer.parseInt(str);
        return defaultValue;
    }
    private static float find (Array<String> lines, String start, float defaultValue) {
        String str = find(lines, start, null);
        if (str != null) return Float.parseFloat(str);
        return defaultValue;
    }
}
