/**
 * AEROSTRIDE - CENTRALIZED ADMIN ICONS CONFIGURATION
 * Use this file to maintain visual consistency across all management modules.
 */

import { 
    PencilIcon, 
    TrashIcon, 
    EyeIcon, 
    PlusIcon, 
    DownloadIcon, 
    UploadIcon,
    SearchIcon,
    RefreshIcon,
    ChevronLeftIcon,
    ChevronRightIcon,
    LayoutGridIcon,
    FilterIcon,
    DotsVerticalIcon,
    CalendarIcon,
    HistoryIcon,
    SettingsIcon,
    FileSpreadsheetIcon,
    MapPinIcon
} from 'vue-tabler-icons';

export const ADMIN_ICONS = {
    // Action Icons (Buttons in tables)
    ACTION: {
        EDIT: PencilIcon,
        DELETE: TrashIcon,
        VIEW: EyeIcon,
        ADD: PlusIcon,
        DETAILS: EyeIcon,
        MORE: DotsVerticalIcon,
        MAP: MapPinIcon
    },
    
    // Feature Icons
    COMMON: {
        SEARCH: SearchIcon,
        REFRESH: RefreshIcon,
        FILTER: FilterIcon,
        GRID: LayoutGridIcon,
        CALENDAR: CalendarIcon,
        HISTORY: HistoryIcon,
        SETTINGS: SettingsIcon,
        EXPORT: FileSpreadsheetIcon,
        IMPORT: UploadIcon,
        DOWNLOAD: DownloadIcon
    },

    // Navigation/Pagination
    NAV: {
        PREV: ChevronLeftIcon,
        NEXT: ChevronRightIcon
    }
};

export default ADMIN_ICONS;
