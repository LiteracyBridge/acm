package org.literacybridge.core.fs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class RelativePath extends ArrayList<String> {
    public static final RelativePath EMPTY = new RelativePath();

    public RelativePath(String... pathSegments) {
        addPathSegments(pathSegments);
    }

    public RelativePath(List<String> pathSegments) {
        super(pathSegments);
    }

    public RelativePath(RelativePath parent, String... pathSegments) {
        super(parent);
        addPathSegments(pathSegments);
    }

//    private RelativePath(List<String> pathSegments, int length) {
//        super(pathSegments.subList(0, length));
//    }

    private void addPathSegments(String... segments) {
        if (segments != null) {
            for (String segment : segments) {
                add(segment);
            }
        }
    }

    public String getLastSegment() {
        return size() > 0 ? get(size() - 1) : "";
    }

    public int getSegmentCount() {
        return size();
    }

    public RelativePath getParent() {
        return size() > 1 ?
               new RelativePath(subList(0, size()-1)) :
               RelativePath.EMPTY;
    }

    public static RelativePath parse(String relativePath) {
        List<String> segments = new ArrayList<String>();
        // A hard way to say "relativePath.split(File.separator)"
        StringTokenizer tokenizer = new StringTokenizer(relativePath, File.separator);
        while (tokenizer.hasMoreTokens()) {
            segments.add(tokenizer.nextToken());
        }
        return new RelativePath(segments);
    }

    public String asString() {
        // If only Android supported remotely modern Java...
        // String result = String.join(File.separator, pathSegments);
        StringBuilder builder = new StringBuilder();
        for (String segment : this) {
            // Only if there's already a segment in place, put in a separator.
            if (builder.length() > 0) {
                builder.append(File.separatorChar);
            }
            builder.append(segment);
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return asString();
    }

}
