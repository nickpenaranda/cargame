package org.cargame;

import java.nio.ByteBuffer;

import org.newdawn.slick.geom.Polygon;

import com.esotericsoftware.kryo.CustomSerialization;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serialize.ArraySerializer;


public class SPolygon extends Polygon implements CustomSerialization {
  private static final long serialVersionUID = 8105288339093694206L;

  public SPolygon(float[] points) {
    super(points);
  }

  public SPolygon() {
    super();
  }

  @Override
  public void readObjectData( Kryo kryo, ByteBuffer buffer ) {
    ArraySerializer arraySerializer = new ArraySerializer(kryo);
    float p[] = arraySerializer.readObjectData(buffer,float[].class);
    for(int i=0;i<p.length;i+=2) {
      addPoint( p[i], p[i + 1] );
    }
  }

  @Override
  public void writeObjectData( Kryo kryo, ByteBuffer buffer ) {
    ArraySerializer arraySerializer = new ArraySerializer(kryo);
    arraySerializer.writeObjectData( buffer, this.getPoints() );
  }

}
