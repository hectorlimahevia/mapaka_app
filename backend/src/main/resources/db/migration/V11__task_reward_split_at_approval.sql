-- El reparto gastar/estalviar d'una recompensa de tasca ja no es demana ni es guarda en
-- crear la tasca: es calcula quan s'aprova, amb el percentatge vigent del fill en aquell
-- moment (pot variar si la tasca s'assigna a diversos fills amb percentatges diferents).
ALTER TABLE task_rewards DROP COLUMN savings_amount;
ALTER TABLE task_completions DROP COLUMN reward_savings;
