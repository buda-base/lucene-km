package io.bdrc.lucene;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;


public class KhmerAnalyzer extends Analyzer {
    
    int normalizationlevel = 0;

    public KhmerAnalyzer() throws IOException {
        this.normalizationlevel = 1;
    }
    
    public KhmerAnalyzer(int normalizationlevel) throws IOException {
        this.normalizationlevel = normalizationlevel;
    }
    
    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        if (this.normalizationlevel > 0)
            reader = new NormalizationCharFilter(reader, this.normalizationlevel);
        return super.initReader(fieldName, reader);
    }
    
    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        Tokenizer source = new GraphemeClusterTokenizer();
        return new TokenStreamComponents(source, new CharReorderFilter(source));
    }
    
}
