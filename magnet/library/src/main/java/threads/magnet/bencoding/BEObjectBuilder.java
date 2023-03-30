package threads.magnet.bencoding;

interface BEObjectBuilder<BEObject> {

    boolean accept(int b);

    BEObject build();

    BEType getType();
}
