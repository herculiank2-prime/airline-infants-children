package com.airline.travel.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.airline.travel.core.models.HeroBanner;
import com.airline.travel.core.models.HeroImage;
import com.airline.travel.core.models.JumpLink;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.ComponentContext;
import com.fasterxml.jackson.annotation.JsonProperty;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = {HeroBanner.class, ComponentExporter.class},
        resourceType = HeroBannerImpl.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class HeroBannerImpl implements HeroBanner {

    static final String RESOURCE_TYPE = "airline/components/hero";
    static final String DEFAULT_JUMP_LINKS_TITLE = "On this page";
    static final String SIZES = "(min-width: 1200px) 50vw, 100vw";
    static final int PREFERRED_SRC_WIDTH = 960;
    static final List<Integer> RENDITION_WIDTHS = List.of(640, 960, 1280);

    private static final Pattern WEB_RENDITION = Pattern.compile("^cq5dam\\.web\\.(\\d+)\\.(\\d+)\\.(?:jpe?g|png|webp)$");
    private static final String NN_JUMP_LINKS = "jumpLinks";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Page currentPage;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private ComponentContext componentContext;

    @ValueMapValue(name = JcrConstants.JCR_TITLE)
    private String title;

    @ValueMapValue
    private String introduction;

    @ValueMapValue
    private String fileReference;

    @ValueMapValue
    private String alt;

    @ValueMapValue
    private boolean isDecorative;

    @ValueMapValue
    private String jumpLinksTitle;

    private HeroImage image;
    private boolean imageMissingAlt;
    private List<JumpLink> jumpLinks = Collections.emptyList();
    private String id;

    @PostConstruct
    protected void init() {
        title = StringUtils.trimToNull(title);
        introduction = StringUtils.trimToNull(introduction);
        jumpLinksTitle = StringUtils.defaultIfBlank(jumpLinksTitle, DEFAULT_JUMP_LINKS_TITLE);
        jumpLinks = readJumpLinks(resource.getChild(NN_JUMP_LINKS));
        initImage();
    }

    private void initImage() {
        if (StringUtils.isBlank(fileReference)) {
            return;
        }
        Resource assetResource = request.getResourceResolver().getResource(fileReference);
        Asset asset = assetResource == null ? null : assetResource.adaptTo(Asset.class);
        if (asset == null) {
            return;
        }
        String resolvedAlt = isDecorative ? "" : resolveAlt(asset);
        if (resolvedAlt == null) {
            imageMissingAlt = true;
            return;
        }
        TreeMap<Integer, Rendition> renditions = webRenditions(asset);
        if (renditions.isEmpty()) {
            return;
        }
        double ratio = aspectRatio(asset);
        List<String> candidates = new ArrayList<>();
        int srcWidth = closestWidth(renditions, PREFERRED_SRC_WIDTH);
        int srcHeight = 0;
        String src = null;
        for (Map.Entry<Integer, Rendition> entry : renditions.entrySet()) {
            int width = entry.getKey();
            String url = request.getResourceResolver().map(request, entry.getValue().getPath());
            candidates.add(url + " " + width + "w");
            if (width == srcWidth) {
                src = url;
                srcHeight = ratio > 0 ? (int) Math.round(width * ratio) : nameHeight(entry.getValue().getName());
            }
        }
        image = new HeroImageImpl(src, String.join(", ", candidates), SIZES, srcWidth, srcHeight,
                resolvedAlt, isDecorative);
    }

    /**
     * Author alt, then the asset's {@code dc:description}. {@code dc:title} is deliberately not used:
     * asset titles are often file names.
     */
    private String resolveAlt(Asset asset) {
        String authored = StringUtils.trimToNull(alt);
        if (authored != null) {
            return authored;
        }
        return StringUtils.trimToNull(asset.getMetadataValue("dc:description"));
    }

    static TreeMap<Integer, Rendition> webRenditions(Asset asset) {
        TreeMap<Integer, Rendition> byWidth = new TreeMap<>();
        List<Rendition> sorted = asset.getRenditions().stream()
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .collect(Collectors.toList());
        for (Rendition rendition : sorted) {
            Matcher matcher = WEB_RENDITION.matcher(rendition.getName());
            if (matcher.matches()) {
                int width = Integer.parseInt(matcher.group(1));
                if (RENDITION_WIDTHS.contains(width)) {
                    byWidth.putIfAbsent(width, rendition);
                }
            }
        }
        return byWidth;
    }

    private static int closestWidth(TreeMap<Integer, Rendition> renditions, int preferred) {
        Integer floor = renditions.floorKey(preferred);
        Integer ceiling = renditions.ceilingKey(preferred);
        if (floor == null) {
            return ceiling;
        }
        if (ceiling == null) {
            return floor;
        }
        return preferred - floor <= ceiling - preferred ? floor : ceiling;
    }

    /**
     * Height over width from the original's TIFF metadata, or 0 when unknown.
     */
    private static double aspectRatio(Asset asset) {
        int width = parseInt(asset.getMetadataValue("tiff:ImageWidth"));
        int height = parseInt(asset.getMetadataValue("tiff:ImageLength"));
        return width > 0 && height > 0 ? (double) height / width : 0;
    }

    private static int nameHeight(String renditionName) {
        Matcher matcher = WEB_RENDITION.matcher(renditionName);
        return matcher.matches() ? Integer.parseInt(matcher.group(2)) : 0;
    }

    private static int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<JumpLink> readJumpLinks(Resource container) {
        if (container == null) {
            return Collections.emptyList();
        }
        List<JumpLink> links = new ArrayList<>();
        Iterator<Resource> children = container.listChildren();
        while (children.hasNext()) {
            ValueMap properties = children.next().getValueMap();
            String label = StringUtils.trimToNull(properties.get("label", String.class));
            String anchor = normaliseAnchor(properties.get("anchor", String.class));
            if (label != null && anchor != null) {
                links.add(new JumpLinkImpl(label, anchor));
            }
        }
        return Collections.unmodifiableList(links);
    }

    /**
     * Strips leading {@code #} and reduces the value to {@code [a-z0-9_-]}, so it can only ever be an
     * in-page fragment.
     */
    static String normaliseAnchor(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String anchor = StringUtils.stripStart(raw.trim(), "#")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("^-+|-+$", "");
        return anchor.isEmpty() ? null : anchor;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getIntroduction() {
        return introduction;
    }

    @Override
    public HeroImage getImage() {
        return image;
    }

    @Override
    public String getJumpLinksTitle() {
        return jumpLinksTitle;
    }

    @Override
    public List<JumpLink> getJumpLinks() {
        return jumpLinks;
    }

    @Override
    public boolean isImageMissingAlt() {
        return imageMissingAlt;
    }

    @Override
    public boolean isEmpty() {
        return title == null;
    }

    @Override
    public String getId() {
        if (id == null) {
            id = ComponentUtils.getId(resource, currentPage, componentContext);
        }
        return id;
    }

    @Override
    @JsonProperty("dataLayer")
    public ComponentData getData() {
        if (!ComponentUtils.isDataLayerEnabled(resource)) {
            return null;
        }
        return DataLayerBuilder.forComponent()
                .withId(this::getId)
                .withType(resource::getResourceType)
                .withTitle(this::getTitle)
                .build();
    }

    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }
}
