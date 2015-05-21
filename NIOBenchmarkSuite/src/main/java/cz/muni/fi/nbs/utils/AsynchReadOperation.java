/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.muni.fi.nbs.utils;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author petom_000
 */
public class AsynchReadOperation {
    public static AtomicInteger completedCounter;
    
    private ByteBuffer buffer;
    private AsynchronousFileChannel channel;
    private int offset;
    private long checkSum;
    private CountDownLatch latch;
    
    public AsynchReadOperation(ByteBuffer buffer, AsynchronousFileChannel channel, int offset) {
        this.buffer= buffer;
        this.channel= channel;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public void countDown() {
        this.latch.countDown();
    }
    
    public static AtomicInteger getCompletedCounter() {
        return completedCounter;
    }
    
    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public AsynchronousFileChannel getChannel() {
        return channel;
    }

    public void setChannel(AsynchronousFileChannel channel) {
        this.channel = channel;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public long getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(long checkSum) {
        this.checkSum = checkSum;
    }
    
}
