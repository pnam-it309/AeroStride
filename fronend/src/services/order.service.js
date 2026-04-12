import { db } from "@/services/mock.db";
import { createMockCrudService } from "@/services/entity.factory";

export const orderService = createMockCrudService({ db, key: "orders" });
