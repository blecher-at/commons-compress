/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.compressors;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.AbstractTestCase;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

public final class BZip2TestCase extends AbstractTestCase {

    @Test
    public void testBzipCreation()  throws Exception {
        File output = null;
        final File input = getFile("test.txt");
        {
            output = new File(dir, "test.txt.bz2");
            final OutputStream out = new FileOutputStream(output);
            final CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream("bzip2", out);
            FileInputStream in = new FileInputStream(input);
            IOUtils.copy(in, cos);
            cos.close();
            in.close();
        }

        final File decompressed = new File(dir, "decompressed.txt");
        {
            final File toDecompress = output;
            final InputStream is = new FileInputStream(toDecompress);
            final CompressorInputStream in =
                new CompressorStreamFactory().createCompressorInputStream("bzip2", is);
            FileOutputStream os = new FileOutputStream(decompressed);
            IOUtils.copy(in, os);
            is.close();
            os.close();
        }

        assertEquals(input.length(),decompressed.length());
    }

    @Test
    public void testBzip2Unarchive() throws Exception {
        final File input = getFile("bla.txt.bz2");
        final File output = new File(dir, "bla.txt");
        final InputStream is = new FileInputStream(input);
        final CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("bzip2", is);
        FileOutputStream os = new FileOutputStream(output);
        IOUtils.copy(in, os);
        is.close();
        os.close();
    }

    @Test
    public void testConcatenatedStreamsReadFirstOnly() throws Exception {
        final File input = getFile("multiple.bz2");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in = new CompressorStreamFactory()
                .createCompressorInputStream("bzip2", is);
            try {
                assertEquals('a', in.read());
                assertEquals(-1, in.read());
            } finally {
                in.close();
            }
        } finally {
            is.close();
        }
    }

    @Test
    public void testConcatenatedStreamsReadFully() throws Exception {
        final File input = getFile("multiple.bz2");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in =
                new BZip2CompressorInputStream(is, true);
            try {
                assertEquals('a', in.read());
                assertEquals('b', in.read());
                assertEquals(0, in.available());
                assertEquals(-1, in.read());
            } finally {
                in.close();
            }
        } finally {
            is.close();
        }
    }

    @Test
    public void testCOMPRESS131() throws Exception {
        final File input = getFile("COMPRESS-131.bz2");
        final InputStream is = new FileInputStream(input);
        try {
            final CompressorInputStream in =
                new BZip2CompressorInputStream(is, true);
            try {
                int l = 0;
                while(in.read() != -1) {
                    l++;
                }
                assertEquals(539, l);
            } finally {
                in.close();
            }
        } finally {
            is.close();
        }
    }

    @Test
    public void testCOMPRESS207Listeners() throws Exception {
        File inputFile = getFile("COMPRESS-207.bz2");
        FileInputStream fInputStream = new FileInputStream(inputFile);
        final List<Integer> blockNumbers = new ArrayList<Integer>();
        final List<Long> readPositions = new ArrayList<Long>();
        final BZip2CompressorInputStream in = new BZip2CompressorInputStream(fInputStream);

        CompressionProgressListener blockListener = new CompressionProgressListener() {

            public void notify(CompressionProgressEvent e) {
                assertSame(in, e.getSource());
                blockNumbers.add(e.getBlockNumber());
                readPositions.add(e.getBytesProcessed());
            }
        };
        in.addCompressionProgressListener(blockListener);

        while(in.read() >= 0);
        in.close();

        // we miss the initial block event which is triggered by the constructor
        assertEquals(4, blockNumbers.size());
        System.err.println(blockNumbers);
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1, blockNumbers.get(i).intValue());
        }

        assertEquals(4, readPositions.size());
        assertEquals(Long.valueOf(899907), readPositions.get(0));
        assertEquals(Long.valueOf(1799817), readPositions.get(1));
        assertEquals(Long.valueOf(2699710), readPositions.get(2));
        assertEquals(Long.valueOf(3599604), readPositions.get(3));
    }

}
