package renderer;

import primitives.Color;
import primitives.Material;
import primitives.Point3D;
import elements.LightSource;
import geometries.Intersectable.GeoPoint;
import primitives.Ray;
import primitives.Util;
import primitives.Vector;
import scene.Scene;
import java.lang.Math;
import java.util.LinkedList;
import java.util.List;

/**
 * A basic class responsible for tracking the ray that inherits from
 * RayTracerBase
 * 
 * @author David and Matan
 *
 */
public class RayTracerBasic extends RayTracerBase {

	private static final int MAX_CALC_COLOR_LEVEL = 10;
	private static final double MIN_CALC_COLOR_K = 0.001;
	private static final double INITIAL_K = 1.0;
	private int numOfRays = 1;

	/**
	 * Ctor - get scene and set it
	 * 
	 * @param scene - body that build from geometries bodies and color and
	 *              ambientLight(strong of the color)
	 */
	public RayTracerBasic(Scene scene) {
		super(scene);
	}

	/**
	 * setter numOfRays for beam
	 * 
	 * @param numRays - number of rays in beam
	 * @throws IllegalArgumentException when numRays smaller than one.
	 * @return RayTracerBasic itself
	 */
	public RayTracerBasic setNumOfRays(int numRays) {
		if (numRays < 1)
			throw new IllegalArgumentException("the number of rays can't smaller than one!");
		numOfRays = numRays;
		return this;
	}

	@Override
	public Color traceRay(Ray ray) {
		var closestPoint = findClosestIntersection(ray);
		if (closestPoint == null)
			return scene.background;
		return calcColor(closestPoint, ray);
	}

	/**
	 * Calculates the color of a given point from camera ray
	 * 
	 * @param ray - ray from the camera
	 * @param geo - point on geometry body
	 * @return the color in this point
	 */
	private Color calcColor(GeoPoint geo, Ray ray) {
		return calcColor(geo, ray, MAX_CALC_COLOR_LEVEL, INITIAL_K).add(scene.ambientLight.getIntensity());
	}

	/**
	 * Recursive function to calculates the color of a given point from camera ray
	 * 
	 * @param closestPoint - point on geometry body
	 * @param ray          - ray from the camera
	 * @param level        - level of Recursion.
	 * @param k            - the current attenuation level
	 * @return the color in this point
	 */
	private Color calcColor(GeoPoint closestPoint, Ray ray, int level, double k) {
		Color color = closestPoint.geometry.getEmmission().add(calcLocalEffects(closestPoint, ray, k));
		return 1 == level ? color : color.add(calcGlobalEffects(closestPoint, ray, level, k));
	}

	/**
	 * calculates light contribution with consideration for transparency and
	 * reflection
	 * 
	 * @param closestPoint - point on geometry body
	 * @param ray          - ray from the camera
	 * @param level        - level of Recursion.
	 * @param k            - the current attenuation level
	 * @return with consideration for transparency and reflection
	 */
	private Color calcGlobalEffects(GeoPoint geopoint, Ray ray, int level, double k) {
		Color color = Color.BLACK;
		Material material = geopoint.geometry.getMaterial();
		double kr = material.kR, kkr = k * kr;
		Vector v = ray.getDir();
		Vector n = geopoint.geometry.getNormal(geopoint.point);
		double nv = Util.alignZero(n.dotProduct(v));
		if (kkr > MIN_CALC_COLOR_K) {
			color = calcGlobalEffect(clacRayReflection(n, v, geopoint.point, nv), n, level, kr, kkr, material.kGS);
		}
		double kt = material.kT, kkt = kt * k;
		if (kkt > MIN_CALC_COLOR_K) {
			color = color
					.add(calcGlobalEffect(clacRayRefraction(n, v, geopoint.point), n, level, kt, kkt, material.kDG));
		}
		return color;
	}

	/**
	 * help function to calculate color of reflected or refracted point
	 * 
	 * @param ray   - ray from the camera
	 * @param n     - vector normal of geometry body in current point
	 * @param level -level of Recursion.
	 * @param kx    - represent the reflection or transparency factor
	 * @param kkx   - k(the current attenuation level) that multiple in "kx"
	 * @param r     - when radius is bigger the impact is more intense
	 * @return the color of reflected or refracted point
	 */
	private Color calcGlobalEffect(Ray ray, Vector n, int level, double kx, double kkx, double r) {
		var color = Color.BLACK;
		var points = ray.createBeam(n, numOfRays, r);
		var p0 = ray.getP0();
		double nv = Util.alignZero(n.dotProduct(ray.getDir()));
		for (var item : points) {
			Vector l = item.subtract(p0);
			double nl = Util.alignZero(n.dotProduct(l));
			if (nv * nl > 0) {
				GeoPoint gp = findClosestIntersection(new Ray(p0, l));
				if (gp != null)
					color = color.add(calcColor(gp, ray, level - 1, kkx).scale(kx));
			}
		}
		var size = points.size();
		return color.add(size > 1 ? color.reduce(size) : color);// return average color by beam
	}

	/**
	 * help to calculate "calcColor" - calculated light contribution from all light
	 * sources
	 * 
	 * @param intersection - point on geometry body
	 * @param ray          - ray from the camera
	 * @param k            - the current attenuation level
	 * @return calculated light contribution from all light sources
	 */
	private Color calcLocalEffects(GeoPoint intersection, Ray ray, double k) {
		Vector v = ray.getDir();
		Vector n = intersection.geometry.getNormal(intersection.point);
		double nv = Util.alignZero(n.dotProduct(v));
		if (nv == 0)
			return Color.BLACK;
		var material = intersection.geometry.getMaterial();
		int nShininess = material.nShininess;
		double kd = material.kD, ks = material.kS;
		Color color = Color.BLACK;
		for (LightSource lightSource : scene.lights) {
			Vector l = lightSource.getL(intersection.point);
			double nl = Util.alignZero(n.dotProduct(l));
			if (nl * nv > 0) { // sign(nl) == sign(nv)
				double ktr = transparency(lightSource, l, n, intersection);
				if (ktr * k > MIN_CALC_COLOR_K) {
					Color lightIntensity = lightSource.getIntensity(intersection.point).scale(ktr);
					color = color.add(calcDiffusive(kd, nl, lightIntensity),
							calcSpecular(ks, n, l, nl, v, nShininess, lightIntensity));
				}
			}
		}
		return color;
	}

	/**
	 * calculate the reflected ray
	 * 
	 * @param n  - normal to the point on geometry
	 * @param v  - camera vector
	 * @param p  - point on geometry body
	 * @param nv - equal to n.dotProduct(v)
	 * @return reflected ray
	 */
	private Ray clacRayReflection(Vector n, Vector v, Point3D p, double nv) {
		Vector r = v.add(n.scale(-2 * nv));
		return new Ray(p, r, n);
	}

	/**
	 * calculate the refracted ray
	 * 
	 * @param n - normal to the point on geometry
	 * @param v - camera vector
	 * @param p - point on geometry body
	 * @return refracted ray
	 */
	private Ray clacRayRefraction(Vector n, Vector v, Point3D p) {
		return new Ray(p, v, n);
	}

	/**
	 * calculate the diffusive light according to Phong's model
	 * 
	 * @param kd             - Coefficient for diffusive
	 * @param nl             - is equal to n.dotProduct(l)
	 * @param lightIntensity - Light intensity
	 * @return the diffusive light
	 */
	private Color calcDiffusive(double kd, double nl, Color lightIntensity) {
		return lightIntensity.scale((nl >= 0 ? nl : -nl) * (kd));
	}

	/**
	 * calculate the specular light according to Phong's model
	 * 
	 * @param ks             - Coefficient for specular
	 * @param l              - vector from light source
	 * @param n              - normal to the point on geometry
	 * @param nl             - is equal to n.dotProduct(l)
	 * @param v              - camera vector
	 * @param nShininess     - exponent
	 * @param lightIntensity - Light intensity
	 * @return the specular light
	 */
	private Color calcSpecular(double ks, Vector n, Vector l, double nl, Vector v, int nShininess,
			Color lightIntensity) {
		Vector r = l.add(n.scale(-2 * nl));
		double vr = Util.alignZero(v.dotProduct(r));
		if (vr >= 0)
			return Color.BLACK;
		return lightIntensity.scale(ks * Math.pow(-vr, nShininess));
	}

	/**
	 * For shading test between point and light source
	 * 
	 * @param light - light source
	 * @param l     - vector from light
	 * @param n     - normal of body
	 * @param gp    - point in geometry body
	 * @return
	 *         <li>true - if unshaded
	 *         <li>false - if shaded
	 */
	private boolean unshaded(LightSource light, Vector l, Vector n, GeoPoint gp) {
		Vector lightDirection = l.scale(-1); // from point to light source
		Ray lightRay = new Ray(gp.point, lightDirection, n);
		var intersections = scene.geometries.findGeoIntersections(lightRay, light.getDistance(gp.point));
		return intersections == null;
	}

	/**
	 * calculates the amount of shadow in the point sometimes we need light shadow
	 * and sometimes not
	 * 
	 * @param light - light source
	 * @param l     - vector from light
	 * @param n     - normal of body
	 * @param gp    - point in geometry body
	 * @return amount of shadow
	 */
	private double transparency(LightSource light, Vector l, Vector n, GeoPoint gp) {
		Vector lightDirection = l.scale(-1); // from point to light source
		Ray lightRay = new Ray(gp.point, lightDirection, n);
		double lightDistance = light.getDistance(gp.point);
		var intersections = scene.geometries.findGeoIntersections(lightRay, lightDistance);
		if (intersections == null)
			return 1.0;
		double ktr = 1.0;
		for (GeoPoint geopoint : intersections) {
			ktr *= geopoint.geometry.getMaterial().kT;
			if (ktr < MIN_CALC_COLOR_K)
				return 0.0;
		}
		return ktr;
	}

	/**
	 * Return the closest intersection point with the ray. if there is no
	 * intersection it returns null
	 * 
	 * @param ray Ray that intersect
	 * @return geoPoint of the closest point
	 */
	private GeoPoint findClosestIntersection(Ray ray) {
		List<GeoPoint> intersections = scene.geometries.findGeoIntersections(ray);
		if (intersections == null) {
			return null;
		}
		return ray.findClosestGeoPoint(intersections);
	}
}
