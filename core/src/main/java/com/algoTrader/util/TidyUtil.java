package com.algoTrader.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

public class TidyUtil {

    private static String[] regexs = new String[] { "<script(.*?)</script>", "<noscript(.*?)</noscript>", "<style(.*?)</style>", "<!--(.*?)-->", "<!(.*?)>",
            "<\\?(.*?)\\?>" };

    private static class NullOutputStream extends OutputStream {

        @Override
        public synchronized void write(byte[] b, int off, int len) {
        }

        @Override
        public synchronized void write(int b) {
        }

        @Override
        public void write(byte[] b) throws IOException {
        }
    }

    private static Tidy _tidy;

    private static Tidy getInstance() {

        if (_tidy == null) {

            _tidy = new Tidy();
            _tidy.setXmlOut(true);
            _tidy.setXHTML(true);
            _tidy.setWord2000(true);
            _tidy.setTidyMark(false);
            _tidy.setLogicalEmphasis(true);
            _tidy.setEncloseText(true);
            _tidy.setQuiet(true);
            _tidy.setShowWarnings(false);
            _tidy.setErrout(new PrintWriter(new NullOutputStream()));
        }

        return _tidy;
    }

    public static Document parse(InputStream in) {

        return getInstance().parseDOM(in, null);
    }

    public static Document parseAndFilter(InputStream in) throws UnsupportedEncodingException, IOException {

        // get the content
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[1024];
        for (int n; (n = in.read(b)) != -1;) {
            out.append(new String(b, 0, n, "UTF-8"));
        }
        in.close();
        String content = out.toString();

        // parse using the regex
        for (String regex : regexs) {
            Pattern noIndexPattern = Pattern.compile(regex, Pattern.DOTALL);
            Matcher noIndexMatcher = noIndexPattern.matcher(content);
            content = noIndexMatcher.replaceAll("");
        }

        // get the document
        return getInstance().parseDOM(new ByteArrayInputStream(content.getBytes()), null);
    }
}
