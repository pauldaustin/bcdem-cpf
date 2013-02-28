package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * The GeometryConfiguration annotation can be used on a {@link BusinessApplicationPlugin} class,
 * a {@link RequestParameter} set method, or a {@link ResultAttribute} get method
 * to indicate the configuration of the required coordinate system,
 * precision model, number of coordinate axis and if the geometry should be
 * validated.</p>
 * 
 * <p>The CPF uses this information to convert the input and result geometries to
 * the correct coordinate system or precision model.</p>
 * 
 * <pre class="prettyprint language-java">&#064;ResultAttribute
&#064;GeometryConfiguration(
  srid = 3005,
  numAxis = 2,
  scaleFactorXy = 1000,
  scaleFactorZ = 1,
  validate = true,
  primaryGeometry = true)
public Geometry getGeometry() {
  return geometry;
}</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {
  ElementType.METHOD, ElementType.TYPE
})
public @interface GeometryConfiguration {
  /**
   * <p>The number or axis used in the geometry. For example a 2D geometry has x, y
   * coordinates, so the number of axis is 2. A 3D geometry has x, y, z so the
   * number of axis is 3. Currently only numAxis of 2 and 3 are supported.</p>
   */
  public int numAxis() default 2;

  /**
   * <p>The flag indicating that this bean property is the primary geometry. This
   * is required as some spatial file formats only support a single real
   * geometry field. Non-primary geometries will be written as an E-WKTR string
   * field. Only one geometry set method and one geometry get method can be
   * marked as primary (the input primary geometry parameter can be different
   * from the result primary geometry attribute.</p>
   */
  public boolean primaryGeometry() default true;

  /**
   * <p>The scale factor to apply the x, y coordinates. The scale factor is 1 /
   * minimum unit. For example if the minimum unit was 1mm (0.001) the scale
   * factor is 1000 (1 / 0.001). The default value 0 indicates a floating
   * precision model where the coordinates are not rounded.</p>
   */
  public double scaleFactorXy() default 0;

  /**
   * <p>The scale factor to apply the z coordinates. The scale factor is 1 /
   * minimum unit. For example if the minimum unit was 1mm (0.001) the scale
   * factor is 1000 (1 / 0.001). The default value 0 indicates a floating
   * precision model where the coordinates are not rounded.</p>
   */
  public double scaleFactorZ() default 0;

  /**
   * <p>The srid of the coordinate system the geometry should be converted to. If
   * this attribute is omitted or has the value 0 then the coordinate system of the source geometry
   * will not be changed. Otherwise it will be projected to the requested
   * coordinate system.</p>
   */
  public int srid() default 0;

  /**
   * <p>Boolean flag indicating if the geometry should be validated using the OGC isValid
   * predicate.</p>
   */
  public boolean validate() default false;
}
