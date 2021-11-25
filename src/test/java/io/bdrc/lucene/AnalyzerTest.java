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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
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
        //assertGraphemeTokenization("ភវីសសាមិមយំបិនសមាកុល",  Arrays.asList("ភ","វីស","សា","មិ","ម","យំ","បិ","ន","ស","មា","កុ","ល"));
    }
    
}