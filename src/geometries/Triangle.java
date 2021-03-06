package geometries;

import java.util.List;

import primitives.Point3D;
import primitives.Ray;
import primitives.Util;

/**
 * A class representing a two-dimensional Triangle in a three-dimensional
 * Cartesian system.
 * <p>
 * based on {@link #Polygon}
 * 
 * @author david and matan
 *
 */
public class Triangle extends Polygon {
	/**
	 * Triangle constructor receiving a 3 Point3d values to Triangle
	 * 
	 * @param point  -a one Vertex value
	 * @param point1 -a one Vertex value
	 * @param point2 -a one Vertex value
	 * @throws IllegalArgumentException
	 *                                  <li>When one of the points is similar to the
	 *                                  other
	 *                                  <li>When they are on the same straight line
	 */
	public Triangle(Point3D point, Point3D point1, Point3D point2) {
		super(point, point1, point2);
	}

	@Override
	public List<GeoPoint> findGeoIntersections(Ray ray, double max) {
		// find intersections
		// if the ray inside the plane - return the cross point
		// if the ray not inside the plane (not cross) - return null
		var myList = super.getPlane().findGeoIntersections(ray, max);
		if (myList == null)
			return null;

		var P0 = ray.getP0();
		var dir = ray.getDir();

		// the formula
		var v1 = vertices.get(0).subtract(P0);
		var v2 = vertices.get(1).subtract(P0);
		var v3 = vertices.get(2).subtract(P0);

		var n1 = v1.crossProduct(v2).normalize();
		var n2 = v2.crossProduct(v3).normalize();
		var n3 = v3.crossProduct(v1).normalize();

		// check if n1,n2,n3 have the same sign(+\-)
		// -- all of them or bigger the zero or smallest then zero --
		if ((Util.alignZero(n1.dotProduct(dir)) > 0 && Util.alignZero(n2.dotProduct(dir)) > 0
				&& Util.alignZero(n3.dotProduct(dir)) > 0) == true
				|| (Util.alignZero(n1.dotProduct(dir)) < 0 && Util.alignZero(n2.dotProduct(dir)) < 0
						&& Util.alignZero(n3.dotProduct(dir)) < 0) == true)
			return List.of(new GeoPoint(this, myList.get(0).point));
		return null;
	}

}