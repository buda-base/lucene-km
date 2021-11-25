package io.bdrc.lucene;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class CharReorderFilter extends TokenFilter  {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    
    protected CharReorderFilter(TokenStream input) {
        super(input);
    }
    
    public enum ChCat {
        OTHER((char)0), BASE((char)1), ROBAT((char)2), COENG((char)3),
        Z((char)4), SHIFT((char)5), VOWEL((char)6), MS((char)7), MF((char)8); 

        public final char value;

        private ChCat(char value) {
            this.value = value;
        }
    }
    
    public static final List<ChCat> categories = Collections.nCopies(93, ChCat.BASE);
    static {
        for (int i = 51 ; i < 69 ; i++)
            categories.set(i, ChCat.VOWEL);
        categories.set(69, ChCat.MS);
        categories.set(70, ChCat.MF);
        categories.set(71, ChCat.MF);
        categories.set(72, ChCat.SHIFT);
        categories.set(73, ChCat.SHIFT);
        categories.set(74, ChCat.MS);
        categories.set(75, ChCat.ROBAT);
        for (int i = 76 ; i < 81 ; i++)
            categories.set(i, ChCat.MS);
        categories.set(81, ChCat.COENG);
        categories.set(82, ChCat.MS);
        for (int i = 83 ; i < 92 ; i++)
            categories.set(i, ChCat.OTHER);
        categories.set(92, ChCat.MS);
    }
    
    public static final ChCat charcat(char c) {
        if ('\u1780' <= c && c <= '\u17DD')
            return categories.get(c-'\u1780');
        if (c == '\u200C' || c == '\u200D')
            return ChCat.Z;
        return ChCat.OTHER;
    }
    
    final static String BNB = "[\u1780-\u1793\u1795-\u17A2]";
    final static String SF = "[\u179E-\u17A0\u17A2]";
    final static String SNF = "[\u1780-\u179D\u17A1]";
    final static String SS = "[\u1784\u1789\u1793\u1794\u1798-\u179D]";
    final static String VA = "[\u17B7-\u17BA\u17BE\u17D0\u17DD]|\u17B6\u17C6";
    
    final static Pattern triisapP = Pattern.compile("{SF}(?:\u17D2{BNB}){{0,2}}|{BNB}(?:\u17D2{SF}(?:\u17D2{BNB})?|\u17D2{BNB}\u17D2{SF}))\u17BB({VA})".replace("{SF}", SF).replace("BNB", BNB).replace("{VA]", VA));
    final static Pattern muusikatoanP = Pattern.compile("({SS}(?:\u17D2{SNF}){{0,2}}|{SNF}(?:\u17D2{SS}(?:\u17D2{SNF})?|\u17D2{SNF}\u17D2{SS}))\u17BB({VA})".replace("{SS}", SS).replace("SNF", BNB).replace("{VA]", VA));
    
    @Override
    public final boolean incrementToken() throws java.io.IOException {
        if (!input.incrementToken()) {
            return false;
        }

        final char[] buffer = termAtt.buffer();
        final int len = termAtt.length();

        if (len < 2 || len > 30)
            return true;
        // if token doesn't start with a base, don't reorder
        if (charcat(buffer[0]) != ChCat.BASE)
            return true;
        
        final char[] cats = new char[len];
        
        for (int i = 0 ; i < len ; i++) {
            ChCat cat = charcat(buffer[i]);
            // Recategorise base → coeng after coeng char
            if (i > 0 && cat == ChCat.BASE && cats[i-1] == ChCat.COENG.value)
                cat = ChCat.COENG;
            cats[i] = cat.value;
        }
        
        final Integer[] indexes = new Integer[len];
        for (Integer i = 0 ; i < len ; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, (a, b) -> cats[a] - cats[b]);
        
        char[] reordered = new char[len];
        for (int i = 0; i < len ; i++) {
            reordered[i] = buffer[indexes[i]];
        }
        
        String res = new String(reordered);
        res = res.replaceAll("([\u200C\u200D])[\u200C\u200D]+", "$1"); // remove multiple ZW(N)J
        res = res.replaceAll("\u17D2\u17D2+", "\u17D2"); // remove multiple coeng (not in document)
        res = res.replaceAll("\u17C1(\u17BB?)\u17B8", "$1\u17BE"); // compose split vowels
        res = res.replaceAll("\u17C1(\u17BB?)\u17B6", "$1\u17C4");
        res = res.replaceAll("\u17B8(\u17BB?)\u17C1", "$1\u17BE");
        res = res.replaceAll("\u17B6(\u17BB?)\u17C1", "$1\u17C4");
        res = res.replaceAll("([\u17B7-\u17BA\u17BE\u17D0\u17DD]|\u17B6\u17C6)(\u17BB)", "$2$1"); // reorder u before VA
        res = triisapP.matcher(res).replaceAll("$1\u17CA$2"); // Upshifting triisap
        res = muusikatoanP.matcher(res).replaceAll("$1\u17C9$2"); // Upshifting muusikatoan
        res = res.replaceAll("(\u17D2\u179A)(\u17D2[\u1780-\u17B3])", "$2$1"); // coeng ro 2nd
        res = res.replaceAll("(\u17D2)\u178A", "$1\u178F"); // coeng da → ta

        final int newlen = res.length();
        if (newlen != len)
            termAtt.setLength(newlen);
        
        // almost magical
        res.getChars(0, newlen, buffer, 0);
        
        return true;
    }
}
