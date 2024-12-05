package org.teacon.powertool.utils.math;

import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.teacon.powertool.client.ClientEvents;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Line3f {
    public final List<LineNode3f> nodes = new ArrayList<>();
    public final Vector3f start;
    public final Vector3f end;
    public final List<Vector3f> line;
    private List<Pair<Vector3f,Vector3f>> vertexesAndNormals;
    
    /**
     * constructor of a line can be rendered in world.
     * @param line a function define domains from zero to one,considering scaling according to line length before call.
     * @param sideCount the number of vertex of each sample point,affects rendering performance.
     * @param radius the radius of the line.
     */
    
    public Line3f(int sideCount, double radius, List<Vector3f> line){
        this.start = line.getFirst();
        this.end = line.getLast();
        this.line = line;
        Vector3f previous = null;
        Vector3f i = null;
        Vector3f k = null;
        Vector3f current;
        Vector3f next;
        for(int n = 0; n < line.size() - 1; n++){
            current = line.get(n);
            next = line.get(n + 1);
            nodes.add(new LineNode3f(current,previous,next,i,k,sideCount,radius));
            previous = current;
            k = nodes.getLast().k;
            i = nodes.getLast().i;
        }
        nodes.add(new LineNode3f(line.getLast(), previous,null,i,k,sideCount,radius));
    }
    
    public List<Pair<Vector3f,Vector3f>> vertexAndNormalQuadsList(){
        if(vertexesAndNormals == null){
            vertexesAndNormals = new ArrayList<>();
            for(var i = 0; i < (nodes.size() - 1); i++){
                var cur = nodes.get(i);
                var next = nodes.get(i + 1);
                var sideCount = cur.sideCount;
                for(int j = 0; j < sideCount; j++){
                    vertexesAndNormals.add(Pair.of(cur.points.get(j),cur.normals.get(j)));
                    vertexesAndNormals.add(Pair.of(next.points.get(j),next.normals.get(j)));
                    var jNext = (j+1)%sideCount;
                    vertexesAndNormals.add(Pair.of(next.points.get(jNext),next.normals.get(jNext)));
                    vertexesAndNormals.add(Pair.of(cur.points.get(jNext),cur.normals.get(jNext)));
                }
            }
            vertexesAndNormals = Collections.unmodifiableList(vertexesAndNormals);
        }
        return vertexesAndNormals;
    }
    
    public static class LineNode3f {
        
        private static final Vector3f ZERO = new Vector3f(0, 0, 0);
        public final int sideCount;
        public final Vector3f center;
        private final List<Vector3f> points = new ArrayList<>();
        private final List<Vector3f> normals = new ArrayList<>();
        private final Vector3f i;
        private final Vector3f k;
        
        public LineNode3f(Vector3f center, @Nullable Vector3f previous, @Nullable Vector3f next, @Nullable Vector3f lastI, @Nullable Vector3f lastK, int sideCount, double radius) {
            assert previous != null || next != null;
            this.sideCount = sideCount;
            this.center = center;
            
            //the facing of line on this center.
            Vector3f j = new Vector3f();
            if(next == null)
                center.sub(previous,j);
            else {
                next.sub(center,j);
            }
            if(j.equals(ZERO)) j.set(0,1,0);
            j.normalize();
            var i = new Vector3f(j.z,0,-j.x);
            if(i.equals(ZERO)) j.set(1,0,0);
            i = compareWithHistory(lastI, i);
            var k = i.cross(j,new Vector3f());
            k = compareWithHistory(lastK, k);
            this.i = i;
            this.k = k;
            var transMatrix = new Matrix3f(i,j,k);
            
            float stepRadian= (float) ((2*Math.PI)/sideCount);
            float startRadian = stepRadian/2;
            for(int n = 0; n < sideCount; n++){
                var rot = startRadian+stepRadian*n;
                var p = new Vector3f((float) (Math.cos(rot)*radius), 0, (float) (Math.sin(rot)*radius));
                p.mul(transMatrix);
                normals.add(p);
                points.add(p.add(center,new Vector3f()));
            }
        }
        
        @NotNull
        private Vector3f compareWithHistory(@Nullable Vector3f lastI, Vector3f i) {
            i.normalize();
            if(lastI != null){
                var negI = i.mul(-1,new Vector3f());
                var d1 = lastI.sub(i,new Vector3f()).lengthSquared();
                var d2 = lastI.sub(negI,new Vector3f()).lengthSquared();
                if(d2 < d1)
                     i = negI;
            }
            return i;
        }
    }
}
