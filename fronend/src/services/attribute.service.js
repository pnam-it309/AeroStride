import { db } from "@/services/mock.db";
import { createMockCrudService } from "@/services/entity.factory";
export const attributeService = createMockCrudService({
  db,
  key: "attributes",
});
