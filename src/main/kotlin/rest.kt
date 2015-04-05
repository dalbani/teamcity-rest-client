package org.jetbrains.teamcity.rest

import retrofit.client.Response
import retrofit.http.*
import retrofit.mime.TypedString
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale

private trait TeamCityService {
    Headers("Accept: application/json")
    GET("/app/rest/builds")
    fun builds(Query("locator") buildLocator: String): BuildInfoListBean

    Headers("Accept: application/json")
    GET("/app/rest/builds/id:{id}")
    fun build(Path("id") id: String): BuildBean

    POST("/app/rest/builds/id:{id}/tags/")
    fun addTag(Path("id") buildId: String, Body tag: TypedString): Response

    PUT("/app/rest/builds/id:{id}/pin/")
    fun pin(Path("id") buildId: String, Body comment: TypedString): Response

    Streaming
    GET("/app/rest/builds/id:{id}/artifacts/content/{path}")
    fun artifactContent(Path("id") buildId: String, Path("path") artifactPath: String): Response

    Headers("Accept: application/json")
    GET("/app/rest/builds/id:{id}/artifacts/children/{path}")
    fun artifactChildren(Path("id") buildId: String, Path("path") artifactPath: String): ArtifactFileListBean

    Headers("Accept: application/json")
    GET("/app/rest/projects/id:{id}")
    fun project(Path("id") id: String): ProjectInfoBean

    Headers("Accept: application/json")
    GET("/app/rest/buildTypes/id:{id}/buildTags")
    fun buildTypeTags(Path("id") buildTypeId: String): TagsBean
}

private val teamCityServiceDateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmssZ", Locale.ENGLISH)

private class ProjectsBean {
    var project: List<ProjectInfoBean> = ArrayList()
}

private class ArtifactFileListBean {
    var file: List<ArtifactFileBean> = ArrayList()
}

private class ArtifactFileBean {
    var name: String? = null
}

private class BuildInfoListBean {
    var build: List<BuildInfoBean> = ArrayList()
}

private open class BuildInfoBean {
    var id: String? = null
    var number: String? = null
    var status: BuildStatus? = null

    fun toBuildInfo(service: TeamCityService): BuildInfoImpl = BuildInfoImpl(
            id = BuildId(id!!),
            service = service,
            buildNumber = number!!,
            status = status!!)
}

private class BuildBean: BuildInfoBean() {
    var queuedDate: String? = null
    var startDate: String? = null
    var finishDate: String? = null

    fun toBuild(service: TeamCityService): Build = BuildImpl(
            buildInfo = toBuildInfo(service),
            startDate = teamCityServiceDateFormat.parse(startDate!!),
            finishDate = teamCityServiceDateFormat.parse(finishDate!!),
            queuedDate = teamCityServiceDateFormat.parse(queuedDate!!))
}

private class BuildTypeInfoBean {
    var id: String? = null
    var name: String? = null
    var projectId: String? = null

    fun toBuildTypeInfo(service: TeamCityService): BuildTypeInfo =
            BuildTypeInfoImpl(BuildTypeId(id!!), name!!, ProjectId(projectId!!), service)
}

private class BuildTypesBean {
    var buildType: List<BuildTypeInfoBean> = ArrayList()
}

private class TagBean {
    var name: String? = null
}

private class TagsBean {
    var tag: List<TagBean>? = ArrayList()
}

private class ProjectInfoBean {
    var id: String? = null
    var name: String? = null
    var parentProjectId: String? = null
    var archived: Boolean = false

    var projects: ProjectsBean? = ProjectsBean()
    var parameters: ParametersBean? = ParametersBean()
    var buildTypes: BuildTypesBean? = BuildTypesBean()

    fun toProjectInfo(service: TeamCityService): ProjectInfo =
            ProjectInfoImpl(ProjectId(id!!), name!!, archived, ProjectId(parentProjectId!!), service)

    fun toProject(service: TeamCityService): Project =
            ProjectImpl(
                    ProjectId(id!!),
                    name!!,
                    archived,
                    ProjectId(parentProjectId!!),
                    projects!!.project.map { it.toProjectInfo(service) },
                    buildTypes!!.buildType.map { it.toBuildTypeInfo(service) },
                    parameters!!.property!!.map { it.toPropertyInfo() }
            )
}

private class ParametersBean {
    var property: List<PropertyBean>? = ArrayList()
}

private class PropertyBean {
    var name: String? = null
    var value: String? = null
    var own: Boolean = false

    fun toPropertyInfo(): PropertyInfo = PropertyInfoImpl(name!!, value, own)
}

