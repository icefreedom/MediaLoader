/**
 * 
 */
package com.lenovo.vcs.medialoader.cache.naming;

import java.net.MalformedURLException;
import java.net.URL;

import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;

/**
 * @author xubin
 *
 */
public class MediaFileNameGenerator implements FileNameGenerator {

    /* (non-Javadoc)
     * @see com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator#generate(java.lang.String)
     */
    @Override
    public String generate(String uri) {
        String suffix = "";
        try {
            URL url = new URL(uri);
            String filename = url.getFile();
            int pos = filename.lastIndexOf('.');
            if (pos != -1) {
                suffix = filename.substring(pos);
            }
        } catch (MalformedURLException e) {
            
        }
        return String.valueOf(uri.hashCode()) + suffix;
    }

}
