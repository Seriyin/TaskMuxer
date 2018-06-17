package pt.um.tf.commons.messaging;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import pt.um.tf.commons.task.Task;

public class UpdateMessage<T extends Task> implements CatalystSerializable {
    private T task;
    private boolean inbound;
    private boolean outbound;
    private boolean more;
    private long sequence;
    private String spreadGroup;

    public T getTask() {
        return task;
    }

    public boolean isInbound() {
        return inbound;
    }

    public boolean isOutbound() {
        return outbound;
    }

    public boolean hasMore() {
        return more;
    }

    public long getSequence() {
        return sequence;
    }

    @Override
    public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
        serializer.writeObject(task);
        buffer.writeBoolean(inbound);
        buffer.writeBoolean(outbound);
        buffer.writeBoolean(more);
        buffer.writeLong(sequence);
    }

    @Override
    public void readObject(BufferInput<?> buffer, Serializer serializer) {
        task = serializer.readObject(buffer);
        inbound = buffer.readBoolean();
        outbound = buffer.readBoolean();
        more = buffer.readBoolean();
        sequence = buffer.readLong();
    }
}
