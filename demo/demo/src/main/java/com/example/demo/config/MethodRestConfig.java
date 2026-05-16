package com.example.demo.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

public class MethodRestConfig implements RepositoryRestConfigurer
{
    private EntityManager entityManager;

    String url="http://localhost:5173";

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        HttpMethod[] chanPhuongThuc = {
                HttpMethod.POST,
                HttpMethod.PUT,
                HttpMethod.PATCH,
                HttpMethod.DELETE,
        };
        //expose id
        config.exposeIdsFor(
                entityManager.getMetamodel()
                        .getEntities()
                        .stream()
                        .map(EntityType::getJavaType)
                        .toArray(Class[]::new)
        );
//        chanHttpMethods(TheLoai.class,config,chanPhuongThuc);

        //Cors Configuration ==> cau hinh fe cho phep truy cap api
        cors.addMapping("/**")
                .allowedOrigins(url)
                .allowedMethods("GET","POST","PUT","DELETE");
    }

    private void chanHttpMethods(Class c ,RepositoryRestConfiguration config,
                                 HttpMethod[] methods
    )
    {
        config.getExposureConfiguration()
                .forDomainType(c)
                .withItemExposure(((metdata, httpMethods) -> httpMethods.disable(methods))
                ).withCollectionExposure((metdata, httpMethods) -> httpMethods.disable(methods));
    }
}
