package Raytracer;

import Light.Light;
import Light.PointLight;
import Objects.Solid;

import java.util.ArrayList;
import java.util.List;

import Math.*;

import static Math.Vec3d.*;
import static Math.Utils.*;
import static java.lang.Math.*;

public class Scene {

    private Camera camera;
    private List<Solid> solids = new ArrayList<>();
    private List<Light> lights = new ArrayList<>();
    private Skybox skybox;

    public Pixel getRayColor(Ray incomingRay, int depth){

        if(depth == 0){
            return new Pixel(skybox.getColor(incomingRay.direction), -1);
        }

        // add info about normal to HITINFO - DONE
        HitInfo hitInfo = getHitInfo(incomingRay);
        double t = hitInfo.t;
        Solid solid = hitInfo.solid;
        Vec3d normal = hitInfo.normal;


        if (solid != null){

            // color of the point hit by incoming ray
            Vec3d hitColor = getHitColor(hitInfo, incomingRay);

            if(solid.getReflectivity() == 0)
                return new Pixel(hitColor, t);

            // reflection color
            Vec3d reflectionColor = getReflectionColor(hitInfo, incomingRay, depth);

            // actual color that comes to the camera
            Vec3d color = add(scale(hitColor, 1 - solid.getReflectivity()), scale(reflectionColor, solid.getReflectivity()));

            return new Pixel(color, t);
        }

        return new Pixel(skybox.getColor(incomingRay.direction), -1);
    }

    private Vec3d getHitColor(HitInfo hitInfo, Ray incomingRay){

        double t = hitInfo.t;
        Solid solid = hitInfo.solid;
        Vec3d hitPoint = incomingRay.at(t);
        Vec3d normal = hitInfo.normal;
        Vec3d color = new Vec3d(0);

        // works only for points light sources
        for(Light lightSource: lights){

            Vec3d lightVec = normalize(subtract(((PointLight) lightSource).position, hitPoint));

            Ray lightRay = new Ray(hitPoint, lightVec);
            HitInfo lightHitInfo = getHitInfo(lightRay);

            if(lightHitInfo.solid != null){
                continue;
            }

            double distanceSquared = lengthSquared(lightVec);
            double lightIntensity =  lightSource.intensity / distanceSquared;

            // Lambertian shading (lambertian coef for material??)
            double lambertianIntensity = lightIntensity * max(0, dot(lightVec, normal));

            // Blinn - Phong (blinn-phong coef material??)
            Vec3d bisector = normalize(add(lightVec, inverse(normalize(incomingRay.direction))));
            double blinnPhongIntensity = 0.2 * lightIntensity * pow(max(0, dot(bisector, normal)), 10);

            Vec3d addColor = scale(scale(lightSource.color, solid.getColor()), lambertianIntensity + blinnPhongIntensity);
            color = add(color, addColor);
        }

        // ambient light -  we need albedo, right now we set it to 1
        color = add(color, solid.getColor());

        return new Vec3d(min(1.0,  color.x), min(1.0,  color.y), min(1.0,  color.z));
    }

    private Vec3d getReflectionColor(HitInfo hitInfo, Ray incomingRay, int depth){

        double t = hitInfo.t;;
        Solid solid = hitInfo.solid;
        Vec3d normal = hitInfo.normal;

        Vec3d dir = normalize(incomingRay.direction);
        Ray newRay = new Ray(incomingRay.at(t), add(subtract(dir, scale(normal, 2 * dot(dir, normal))), scale(randomInSphere(), solid.getRoughness())));

        return getRayColor(newRay, depth - 1).getColor();
    }

    private HitInfo getHitInfo(Ray ray){
        HitInfo hitInfo = new HitInfo(99999, null, null);

        for (Solid solid : solids) {

            HitInfo tempHitInfo = solid.calculateIntersection(ray);

            if (tempHitInfo.t >= 0.001 && tempHitInfo.t <= hitInfo.t) {
                hitInfo = tempHitInfo;
            }
        }

        return hitInfo;
    }

    public Scene(){
        camera = new Camera(new Vec3d(0,0,10));
        skybox = new Skybox("Sky.jpg");
    }

    public void addSolid(Solid solid){
        solids.add(solid);
    }

    public void addLight(Light light){
        lights.add(light);
    }

    public Camera getCamera() {
        return camera;
    }

    public Skybox getSkybox() {
        return skybox;
    }
}
