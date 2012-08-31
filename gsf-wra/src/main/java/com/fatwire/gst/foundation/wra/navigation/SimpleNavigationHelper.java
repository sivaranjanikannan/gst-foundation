/*
 * Copyright 2010 FatWire Corporation. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fatwire.gst.foundation.wra.navigation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import COM.FutureTense.Interfaces.ICS;
import COM.FutureTense.Interfaces.Utilities;

import com.fatwire.assetapi.data.AssetId;
import com.fatwire.cs.core.db.PreparedStmt;
import com.fatwire.cs.core.db.StatementParam;
import com.fatwire.gst.foundation.facade.assetapi.asset.TemplateAsset;
import com.fatwire.gst.foundation.facade.assetapi.asset.TemplateAssetAccess;
import com.fatwire.gst.foundation.facade.runtag.render.LogDep;
import com.fatwire.gst.foundation.facade.sql.IListIterable;
import com.fatwire.gst.foundation.facade.sql.Row;
import com.fatwire.gst.foundation.facade.sql.SqlHelper;
import com.fatwire.gst.foundation.facade.uri.TemplateUriBuilder;
import com.fatwire.gst.foundation.wra.WraUriBuilder;

/**
 * To retrieve the Navigation Bar data.
 * 
 * 
 * @author Dolf Dijkstra
 * @since August 31,2012
 */
public class SimpleNavigationHelper implements NavigationService {

    protected static final Log LOG = LogFactory.getLog(SimpleNavigationHelper.class);
    private final ICS ics;
    private final TemplateAssetAccess assetTemplate;

    private String linkLabelAttribute = "linktext";

    private String pathAttribute = "path";

    /**
     * Constructor.
     * 
     * @param ics object
     */
    public SimpleNavigationHelper(final ICS ics) {
        this.ics = ics;
        this.assetTemplate = new TemplateAssetAccess(ics);

    }

    /**
     * 
     * 
     * @param ics
     * @param assetTemplate
     */
    public SimpleNavigationHelper(ICS ics, TemplateAssetAccess assetTemplate) {
        this.ics = ics;
        this.assetTemplate = assetTemplate;
    }

    public SimpleNavigationHelper(ICS ics, TemplateAssetAccess assetTemplate, String linkLabelAttribute,
            String pathAttribute) {
        if (StringUtils.isBlank(linkLabelAttribute))
            throw new IllegalArgumentException("linkLabelAttribute cannot be blank");
        if (StringUtils.isBlank(pathAttribute))
            throw new IllegalArgumentException("pathAttribute cannot be blank");

        this.ics = ics;
        this.assetTemplate = assetTemplate;
        this.pathAttribute = pathAttribute;
        this.linkLabelAttribute = linkLabelAttribute;
    }

    /**
     * @param site
     * @return the root SitePlanTree nodes for this site
     */
    public Collection<NavigationNode> getRootNodesForSite(String site) {

        return getRootNodesForSite(site, -1);

    }

    public Collection<NavigationNode> getRootNodesForSite(String site, int depth) {

        String rootSql = "SELECT nid FROM SitePlanTree WHERE otype='Publication' AND exists (SELECT 1 FROM Publication WHERE name=? AND id=SitePlanTree.oid)";

        PreparedStmt stmt = new PreparedStmt(rootSql, Arrays.asList("SitePlanTree", "Publication"));
        stmt.setElement(0, "Publication", "name");
        StatementParam param = stmt.newParam();
        param.setString(0, site);
        Row root = SqlHelper.selectSingle(ics, stmt, param);
        if (root != null) {

            Long nid = root.getLong("nid");
            return getNodeChildren(nid, 0, depth);
        } else {
            LOG.debug("No root SitePlanTree nodes found for site " + site);
        }
        return Collections.emptyList();

    }

    @Override
    public NavigationNode getNodeByName(String pagename, String site, int depth) {

        String rootSql = "SELECT nid FROM SitePlanTree WHERE EXISTS( SELECT 1 FROM Page p ,AssetPublication ap , Publication pub WHERE p.name=? AND pub.name=? AND ap.assetid=p.id AND pub.id = ap.pubid  AND SitePlanTree.oid = p.id) AND ncode='Placed' ORDER BY nrank)";

        PreparedStmt stmt = new PreparedStmt(rootSql, Arrays.asList("SitePlanTree", "Page", "AssetPublication",
                "Publication"));
        stmt.setElement(0, "Page", "name");
        stmt.setElement(1, "Publication", "name");

        StatementParam param = stmt.newParam();
        param.setString(1, pagename);
        param.setString(0, site);

        Row root = SqlHelper.selectSingle(ics, stmt, param);
        if (root != null) {
            Long nid = root.getLong("nid");
            Collection<NavigationNode> children = getNodeChildren(nid, 0, depth);
            if (!children.isEmpty())
                return children.iterator().next();
        } else {
            LOG.debug("No root SitePlanTree nodes found for site " + site);
        }
        return null;

    }

    /**
     * List all the Page assets at this SitePlanTree nodeId.
     * 
     * @param nodeId the nodeId from the SitePlanTree
     * @param level the tree level depth
     * @param depth the maximum depth
     * @return
     */

    protected Collection<NavigationNode> getNodeChildren(final long nodeId, final int level, final int depth) {
        String sql = "SELECT otype,oid,nrank,nid from SitePlanTree where nparentid=? and ncode='Placed' order by nrank";
        PreparedStmt stmt = new PreparedStmt(sql, Arrays.asList("SitePlanTree"));
        stmt.setElement(0, "SitePlanTree", "nparentid");
        StatementParam param = stmt.newParam();
        param.setLong(0, nodeId);

        IListIterable root = SqlHelper.select(ics, stmt, param);
        List<NavigationNode> collection = new LinkedList<NavigationNode>();
        for (Row row : root) {

            long nid = row.getLong("nid");
            long pageId = row.getLong("oid");

            AssetId pid = assetTemplate.createAssetId(row.getString("otype"), pageId);
            LogDep.logDep(ics, pid);
            TemplateAsset asset = assetTemplate.read(pid, "name", "subtype", "template", pathAttribute,
                    linkLabelAttribute);

            final NavigationNode node = new NavigationNode();

            node.setPage(pid);
            node.setLevel(level);
            node.setPagesubtype(asset.getSubtype());
            node.setPagename(asset.asString("name"));
            node.setId(asset.getAssetId());
            final String url = getUrl(asset);

            if (url != null) {
                node.setUrl(url);
            }

            final String linktext = asset.asString(linkLabelAttribute);

            if (linktext != null) {
                node.setLinktext(linktext);
            } else {
                node.setLinktext(asset.asString("name"));
            }
            if (depth < 0 || depth > level) {
                // get the children in the Site Plan, note recursing here
                Collection<NavigationNode> children = getNodeChildren(nid, level + 1, depth);
                for (final NavigationNode kid : children) {
                    if (kid != null && kid.getPage() != null) {
                        node.addChild(kid);
                    }
                }
            }
        }
        return collection;
    }

    protected String getUrl(TemplateAsset asset) {
        String template = asset.asString("template");
        String path = asset.asString(pathAttribute);
        if (StringUtils.isBlank(template)) {
            LOG.warn("Asset " + asset.getAssetId() + " does not have a valid template set.");
            return null;
        }
        if (StringUtils.isBlank(path)) {
            LOG.warn("Asset " + asset.getAssetId() + " does not have a valid path set. Defaulting to a non Vanity Url.");
            return new TemplateUriBuilder(asset.getAssetId(), template).toURI(ics);
        }

        String wrapper = ics.GetProperty("com.fatwire.gst.foundation.url.wrapathassembler.dispatcher",
                "ServletRequest.properties", true);
        if (!Utilities.goodString(wrapper)) {
            wrapper = "GST/Dispatcher";
        }
        return new WraUriBuilder(asset.getAssetId()).wrapper(wrapper).template(template).toURI(ics);

    }

    /**
     * @return the linkLabelAttribute
     */
    public String getLinkLabelAttribute() {
        return linkLabelAttribute;
    }

    /**
     * @param linkLabelAttribute
     */
    public void setLinkLabelAttribute(String linkLabelAttribute) {
        this.linkLabelAttribute = linkLabelAttribute;
    }

    /**
     * @return the pathAttribute
     */
    public String getPathAttribute() {
        return pathAttribute;
    }

    /**
     * @param pathAttribute
     */
    public void setPathAttribute(String pathAttribute) {
        this.pathAttribute = pathAttribute;
    }
}
