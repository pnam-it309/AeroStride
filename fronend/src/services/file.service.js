import { db } from "@/services/mock.db";
import { createMockCrudService } from "@/services/entity.factory";
export const fileService = createMockCrudService({ db, key: "files" });
