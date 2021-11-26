package io.bdrc.lucene.km;

import java.io.IOException;

import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.CharacterUtils.CharacterBuffer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * Tokenizes a string in Khmer grapheme clusters (not phonetic syllables), for instance:
 * "ខ្ញុំចង់ធ្វើការ" will be tokenized as "ខ្ញុំ", "ច", "ង់", "ធ្វើ", "កា", "រ", not "ខ្ញុំ", "ចង់", "ធ្វើ", "ការ". It uses a simple state machine to do so.
 * 
 */

public class GraphemeClusterTokenizer  extends Tokenizer {

    
    private int offset = 0, bufferIndex = 0, dataLen = 0, finalOffset = 0;
    public static final int DEFAULT_MAX_WORD_LEN = 255;
    private static final int IO_BUFFER_SIZE = 4096;
    private final int maxTokenLen = 255;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private final CharacterBuffer ioBuffer = CharacterUtils.newCharacterBuffer(IO_BUFFER_SIZE);
    
    /**
     * Construct a new TibSyllableTokenizer.
     */
    public GraphemeClusterTokenizer() {
    }

    // states
    public final static int ST_INSIDESYL = 1;
    public final static int ST_AFTERCOENG = 2;
    public final static int ST_AFTERDIGIT = 3;
    public final static int ST_INIT = 4;
    
    // char categories
    public final static int CHCAT_BASE = 1; // consonnants or independent vowels ("base")
    public final static int CHCAT_INSIDE = 2; // anything that can be inside a syllable after the base
    public final static int CHCAT_COENG = 3; // coeng
    public final static int CHCAT_DIGIT = 4; // digit
    public final static int CHCAT_IGNORE = 5; // ignore (punctuation and the rest)
    
    public static final int category(int c) {
        if (('\u17E0' <= c && c <= '\u17F9') || ('0' <= c && c <= '9')) return CHCAT_DIGIT;
        if ('\u1780' <= c && c <= '\u17B3') return CHCAT_BASE;
        if (c == '\u17D2') return CHCAT_COENG;
        if (('\u17B6' <= c && c <= '\u17D3') || c == '\u17DD' || c == '\u200C' || c == '\u200D') return CHCAT_INSIDE;
        return CHCAT_IGNORE;
    }
    
    @Override
    public final boolean incrementToken() throws IOException {
      clearAttributes();
      int length = 0;
      int start = -1; // this variable is always initialized
      int end = -1;
      char[] buffer = termAtt.buffer();
      int state = ST_INIT;
      while (true) {
        if (bufferIndex >= dataLen) {
          offset += dataLen;
          CharacterUtils.fill(ioBuffer, input); // read supplementary char aware with CharacterUtils
          if (ioBuffer.getLength() == 0) {
            dataLen = 0; // so next offset += dataLen won't decrement offset
            if (length > 0) {
              break;
            } else {
              finalOffset = correctOffset(offset);
              return false;
            }
          }
          dataLen = ioBuffer.getLength();
          bufferIndex = 0;
        }
        // use CharacterUtils here to support < 3.1 UTF-16 code unit behavior if the char based
        // methods are gone
        final int c = Character.codePointAt(ioBuffer.getBuffer(), bufferIndex, ioBuffer.getLength());
        final int charCount = Character.charCount(c);
        bufferIndex += charCount;
        
        final int charcat = category(c);
        
        if (charcat == CHCAT_IGNORE) {
            if (length > 0)
                break;
            else
                continue;
        }
        
        //System.out.println(String.format("char %x , charcat %d, state %d", c, charcat, state));
        
        // break before the next character
        boolean breakB = false;
        
        /*
         * afterother+other=nocut,other
         * afterother+base=cut,inside
         * afterother+digit=cut,afterdigit
         * afterother+coeng=nocut,other
         * 
         * inside+coeng=nocut,aftercoeng
         * inside+other=cut,other
         * inside+base=cut,inside
         * inside+digit=cut,afterdigit
         * inside+inside=nocut,inside
         * 
         * aftercoeng+base=nocut,inside
         * aftercoeng+digit=cut,afterdigit
         * aftercoeng+other=cut,other
         * aftercoeng+inside=nocut,inside
         * aftercoeng+coeng=nocut,inside
         * 
         * afterdigit+digit=nocut,afterdigit
         * afterdigit+base=cut,inside
         * afterdigit+other=cut,other
         * afterdigit+inside=cut,inside(error?)
         * afterdigit+coeng=cut,other(error?)
         */
        
        switch (state) {
        case ST_INIT:
            if (charcat == CHCAT_BASE) {
                breakB = true;
                state = ST_INSIDESYL;
            } else if (charcat == CHCAT_DIGIT) {
                breakB = true;
                state = ST_AFTERDIGIT;
            }
            break;
        case ST_INSIDESYL:
            if (charcat == CHCAT_COENG)
                state = ST_AFTERCOENG;
            else if (charcat != CHCAT_INSIDE)
                breakB = true;
            break;
        case ST_AFTERCOENG:
            if (charcat == CHCAT_DIGIT)
                breakB = true;
            else if (charcat != CHCAT_COENG)
                state = ST_INSIDESYL;
            break;
        case ST_AFTERDIGIT:
            if (charcat != CHCAT_DIGIT)
                breakB = true;
            break;
        }
        
        //System.out.println(String.format("   => break %b , state %d, length %d", breakB, state, length));
        
        if (breakB && length > 0) {
            bufferIndex -= charCount;
            break;
        }

        if (length == 0) { // start of token
          start = offset + bufferIndex - charCount;
          end = start;
          switch(charcat) {
          case CHCAT_BASE:
              state = ST_INSIDESYL;
              break;
          case CHCAT_DIGIT:
              state = ST_AFTERDIGIT;
              break;
          }
        } else if (length >= buffer.length - 1) { // supplementary could run out of bounds?
          // make sure a supplementary fits in the buffer
          buffer = termAtt.resizeBuffer(2 + length);
        }
        
        end += charCount;
        length += Character.toChars(c, buffer, length); // buffer it, normalized
        // buffer overflow! make sure to check for >= surrogate pair could break == test
        // XXX: manual change here to cut after 0F7F
        if (c == '\u0f7f' || length >= maxTokenLen) {
          break;
        }
      }

      termAtt.setLength(length);
      offsetAtt.setOffset(correctOffset(start), finalOffset = correctOffset(end));
      return true;
    }

    @Override
    public final void end() throws IOException {
      super.end();
      // set final offset
      offsetAtt.setOffset(finalOffset, finalOffset);
    }

    @Override
    public void reset() throws IOException {
      super.reset();
      bufferIndex = 0;
      offset = 0;
      dataLen = 0;
      finalOffset = 0;
      ioBuffer.reset(); // make sure to reset the IO buffer!!
    }
    
}
