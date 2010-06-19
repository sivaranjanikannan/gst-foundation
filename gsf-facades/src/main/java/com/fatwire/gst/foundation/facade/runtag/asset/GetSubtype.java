package com.fatwire.gst.foundation.facade.runtag.asset;

import COM.FutureTense.Interfaces.ICS;

import com.fatwire.assetapi.data.AssetId;
import com.fatwire.gst.foundation.facade.runtag.AbstractTagRunner;
import com.openmarket.xcelerate.asset.AssetIdImpl;

/**
 * <ASSET.GETSUBTYPE [NAME="loaded asset]" [TYPE="assettype]"
 * [OBJECTID="asset id]" [OUTPUT="variable name"] />
 * 
 * @author Tony Field
 * @since Oct 7, 2008
 */
public class GetSubtype extends AbstractTagRunner {
    public GetSubtype() {
        super("ASSET.GETSUBTYPE");
    }

    public void setAssetId(AssetId id) {
        setType(id.getType());
        setObjectid(id.getId());
    }

    public void setName(String s) {
        this.set("NAME", s);
    }

    public void setType(String s) {
        this.set("TYPE", s);
    }

    public void setObjectid(long id) {
        this.set("OBJECTID", Long.toString(id));
    }

    public void setOutput(String s) {
        this.set("OUTPUT", s);
    }

    /**
     * Get the subtype of the specified asset. The asset does not need to be
     * loaded.
     * 
     * @param ics ICS context
     * @param id the Id of the asset to return the subtype for.
     * @return subtype on success
     */
    public static String getSubtype(ICS ics, AssetId id) {
        ics.PushVars();
        GetSubtype gs = new GetSubtype();
        gs.setAssetId(id);
        gs.setOutput("st");
        gs.execute(ics);
        String ret = ics.GetVar("st");
        ics.PopVars();
        return ret;
    }

    /**
     * Get the subtype of the specified asset. The asset does not need to be
     * loaded.
     * 
     * @param ics ICS context
     * @param c the type of the asset to return the subtype for.
     * @param cid the id of the asset to return the subtype for.
     * @return subtype on success
     */
    public static String getSubtype(ICS ics, String c, String cid) {
        return getSubtype(ics, new AssetIdImpl(c, Long.valueOf(cid)));
    }

    /**
     * Get the subtype for the specified loaded asset.
     * 
     * @param ics ICS context
     * @param loadedAssetName object pool handle for the asset
     * @return the subtype on success
     */
    public static String getSubtype(ICS ics, String loadedAssetName) {
        ics.PushVars();
        GetSubtype gs = new GetSubtype();
        gs.setName(loadedAssetName);
        gs.setOutput("st");
        gs.execute(ics);
        String ret = ics.GetVar("st");
        ics.PopVars();
        return ret;
    }
}
