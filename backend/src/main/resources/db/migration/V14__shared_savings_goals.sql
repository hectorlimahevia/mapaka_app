-- Compartir un objectiu d'estalvi entre germans: cada germà manté el seu propi
-- SavingsGoal (progrés individual, mai un pot compartit), però totes les còpies
-- creades a partir de la mateixa invitació comparteixen shared_goal_group_id
-- (mateix patró que task_completions.completion_group_id).
ALTER TABLE savings_goals ADD COLUMN shared_goal_group_id UUID NULL;
CREATE INDEX savings_goals_shared_goal_group_id_idx ON savings_goals (shared_goal_group_id);

CREATE TYPE goal_invitation_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED');

CREATE TABLE savings_goal_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shared_goal_group_id UUID NOT NULL,
    source_goal_id UUID NOT NULL REFERENCES savings_goals(id),
    inviter_child_id UUID NOT NULL REFERENCES child_profiles(id),
    invited_child_id UUID NOT NULL REFERENCES child_profiles(id),
    status goal_invitation_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at TIMESTAMPTZ,
    CONSTRAINT savings_goal_invitations_unique_invite UNIQUE (shared_goal_group_id, invited_child_id)
);

CREATE INDEX savings_goal_invitations_invited_child_id_idx ON savings_goal_invitations (invited_child_id);
