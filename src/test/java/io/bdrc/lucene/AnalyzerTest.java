package io.bdrc.lucene;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

public class AnalyzerTest {
    
    static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
        tokenizer.close();
        tokenizer.end();
        tokenizer.setReader(reader);
        tokenizer.reset();
        return tokenizer;
    }
    
    static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
        try {
            List<String> termList = new ArrayList<String>();
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                termList.add(charTermAttribute.toString());
            }
            System.out.println(String.join(" ", termList));
            assertThat(termList, is(expected));
        } catch (IOException e) {
            assertTrue(false);
        }
    }
    
    public void assertGraphemeTokenization(final String s, final List<String> expected) throws IOException {
        Reader reader = new StringReader(s);
        System.out.print(s + " => ");
        TokenStream res = tokenize(reader, new GraphemeClusterTokenizer());
        assertTokenStream(res, expected);
    }
    
    @Test
    public void graphemeTokenizerTest() throws IOException {
        System.out.println("Testing GraphemeClusterTokenizer()");
        //assertGraphemeTokenization("ខ្ញុំច",  Arrays.asList("ខ្ញុំ", "ច"));
        //assertGraphemeTokenization("ខ្ញុំចង់ធ្វើការ",  Arrays.asList("ខ្ញុំ", "ច", "ង់", "ធ្វើ", "កា", "រ"));
        assertGraphemeTokenization("ខ្ញុំ ច_ង់៕ធ្វើការ",  Arrays.asList("ខ្ញុំ", "ច", "ង់", "ធ្វើ", "កា", "រ"));
        assertGraphemeTokenization("ភវីសសាមិមយំបិនសមាកុល",  Arrays.asList("ភ","វី","ស","សា","មិ","ម","យំ","បិ","ន","ស","មា","កុ","ល"));
    }
    
    public void assertReorder(final String s, final List<String> expected) throws IOException {
        Reader reader = new StringReader(s);
        System.out.print(s + " => ");
        TokenStream toks = tokenize(reader, new WhitespaceTokenizer());
        CharReorderFilter res = new CharReorderFilter(toks);
        assertTokenStream(res, expected);
    }
    
    @Test
    public void CharReorderTest() throws IOException {
        System.out.println("Testing CharReorderFilter()");
        assertReorder("ស្រ្តី ស្ត្រី ស្រ្ដី ស្ដ្រី ស្រី្ត ស្តី្រ ស្រី្ដ ស្ដី្រ សី្ត្រ  សី្ដ្រ សី្រ្ត សី្រ្ដ ",  Arrays.asList("ស្ត្រី", "ស្ត្រី", "ស្ត្រី", "ស្ត្រី", "ស្ត្រី", "ស្ត្រី", "ស្ត្រី", "ស្ត្រី", "ស្ត្រី", "ស្ត្រី", "ស្ត្រី", "ស្ត្រី"));
        assertReorder("ស៊ើ សើុ ស៊េី សេីុ សុើ",  Arrays.asList("ស៊ើ", "ស៊ើ", "ស៊ើ", "ស៊ើ", "ស៊ើ"));
    }
    
    public void assertFullAnalysis(final String s, final List<String> expected) throws IOException {
        Reader reader = new StringReader(s);
        System.out.print(s + " => ");
        TokenStream toks = tokenize(new NormalizationCharFilter(reader, 2), new WhitespaceTokenizer());
        CharReorderFilter res = new CharReorderFilter(toks);
        assertTokenStream(res, expected);
    }
    
    @Test
    public void FullAnalysisTest() throws IOException {
        System.out.println("Testing full analyzer");
        assertFullAnalysis("ស៊ើ សើុ ស៊េី សេីុ សុើ",  Arrays.asList("ស៊ើ", "ស៊ើ", "ស៊ើ", "ស៊ើ", "ស៊ើ"));
    }
    
}
