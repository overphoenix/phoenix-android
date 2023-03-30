package tech.nagual.phoenix.navigation

import me.zhanghai.android.files.navigation.NavigationItemListLiveData
import tech.nagual.phoenix.tools.organizer.OrganizersManager
import tech.nagual.phoenix.tools.organizer.data.model.Organizer

class NavigationItemListLiveData :
    NavigationItemListLiveData(),
    OrganizersManager.OrganizersListener {
    init {
        OrganizersManager.getInstance().addOrganizersListener(this)
    }

    override fun loadValue() {
        value = navigationItems
    }

    override fun updateOrganizers(organizers: List<Organizer>) {
        loadValue()
    }
}
