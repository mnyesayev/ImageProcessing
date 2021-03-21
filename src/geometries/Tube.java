package geometries;

import java.util.List;

import primitives.*;

/**
 * A class representing a Tube in a three-dimensional Cartesian system
 * 
 * @author David and Matan
 *
 */
public class Tube implements Geometry {

	protected Ray axisRay;
	protected double radius;

	/**
	 * getter the Ray of Tube
	 * 
	 * @return a Axis Ray of Tube
	 */
	public Ray getAxisRay() {
		return axisRay;
	}

	/**
	 * getter the radius of Tube
	 * 
	 * @return a radius of Tube
	 */
	public double getRadius() {
		return radius;
	}

	/**
	 * Tube constructor receiving a Ray value and radius.
	 * 
	 * @param axisRay - a Axis Ray of Tube
	 * @param radius  - a radius of Tube
	 */
	public Tube(Ray axisRay, double radius) {
		this.axisRay = axisRay;
		this.radius = radius;
	}

	@Override
	public Vector getNormal(Point3D point) {
		Vector dir = axisRay.getDir();
		Point3D p0 = axisRay.getP0();
		
		var t = dir.dotProduct(point.subtract(p0));
		if(Util.isZero(t))
			return point.subtract(p0).normalize();
		var o = p0.add(dir.scale(t));
		return point.subtract(o).normalize();
	}

	@Override
	public String toString() {
		return "Ray= " + axisRay + ", radius= " + radius;
	}

	@Override
	public List<Point3D> findIntersections(Ray ray) {
		// TODO Auto-generated method stub
		return null;
	}
}
