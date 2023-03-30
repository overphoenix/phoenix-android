package threads.magnet.data;

import threads.magnet.data.range.Range;

public interface DataRange extends Range<DataRange> {

    void visitUnits(DataRangeVisitor visitor);

    DataRange getSubrange(long offset, long length);

    DataRange getSubrange(long offset);
}
