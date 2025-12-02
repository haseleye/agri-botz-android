package com.example.agribotz.app.network

import com.example.agribotz.app.domain.GPS
import com.example.agribotz.app.domain.Gadget
import com.example.agribotz.app.domain.Site
import com.example.agribotz.app.domain.User
import com.example.agribotz.app.domain.Variable

data class LoginRequest(
    val mobileNumber: String,
    val password: String
)

data class LoginMessage(
    val user: User,
    val sites: List<Site>,
    val accessToken: String,
    val renewToken: String
)

data class LoginResponse(
    val status: String,
    val error: String,
    val message: LoginMessage
)

data class GetSitesRequest(
    val userId: String?
)
data class GetSitesMessage(
    val sites: List<Site>
)

data class GetSitesResponse(
    val status: String,
    val error: String,
    val message: GetSitesMessage
)

data class AddSiteRequest(
    val siteName: String
)

data class SiteMessage(
    val siteInfo: Site
)

data class AddSiteResponse(
    val status: String,
    val error: String,
    val message: SiteMessage
)

data class DeleteSiteRequest(
    val siteId: String,
)

data class DeleteSiteResponse(
    val status: String,
    val error: String,
    val message: Any?
)

data class RenameSiteRequest(
    val siteId: String,
    val newName: String
)

data class RenameSiteResponse(
    val status: String,
    val error: String,
    val message: SiteMessage
)

data class SiteInfoRequest(
    val siteId: String,
)

data class SiteInfoMessage(
    val siteInfo: Site,
    val gadgets: List<Gadget>
)

data class SiteInfoResponse(
    val status: String,
    val error: String,
    val message: SiteInfoMessage
)

data class GadgetInfoRequest(
    val gadgetId: String,
)

data class GadgetInfoMessage(
    val gadgetInfo: Gadget
)

data class GadgetInfoResponse(
    val status: String,
    val error: String,
    val message: GadgetInfoMessage
)

data class RenameGadgetRequest(
    val gadgetId: String,
    val newName: String
)

data class RenameGadgetResponse(
    val status: String,
    val error: String,
    val message: String?
)

data class GadgetGpsRequest(
    val gadgetId: String,
    val gps: GPS
)

data class GadgetGpsResponse(
    val status: String,
    val error: String,
    val message: String?
)

data class UpdateVariableRequest(
    val variableId: String,
    val value: Variable
)

data class UpdateVariableResponse(
    val status: String,
    val error: String,
    val message: String?
)
