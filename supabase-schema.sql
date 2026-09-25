-- SellerNote / Mal Ucotu Supabase setup
-- Run this in Supabase SQL Editor for project qzrysfkotffuajczxjnv.
-- Keep service/secret keys out of the frontend and out of this public repo.

create extension if not exists pgcrypto;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create table if not exists public.categories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists categories_user_name_unique
  on public.categories (user_id, lower(name));

create table if not exists public.products (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category_id uuid references public.categories(id) on delete set null,
  name text not null default 'Adsiz mehsul',
  sale_price numeric(12,2) not null default 0 check (sale_price >= 0),
  cost_price numeric(12,2) not null default 0 check (cost_price >= 0),
  note text,
  image_url text,
  image_urls text[] not null default '{}',
  deleted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists products_user_created_idx
  on public.products (user_id, created_at desc);

create index if not exists products_user_category_idx
  on public.products (user_id, category_id);

create table if not exists public.sales (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  product_id uuid references public.products(id) on delete set null,
  category_id uuid references public.categories(id) on delete set null,
  product_name text not null default 'Satis',
  quantity integer not null default 1 check (quantity > 0),
  unit_sale_price numeric(12,2) not null default 0,
  unit_cost_price numeric(12,2) not null default 0 check (unit_cost_price >= 0),
  sale_price_provided boolean not null default true,
  cost_price_provided boolean not null default true,
  payment_method text not null default 'cash' check (payment_method in ('cash','card')),
  card_account text,
  money_holder text not null default 'normal' check (money_holder in ('normal','yusif')),
  is_checked boolean not null default false,
  total_revenue numeric(12,2) generated always as (
    case when sale_price_provided then quantity * unit_sale_price else 0 end
  ) stored,
  total_cost numeric(12,2) generated always as (
    case when cost_price_provided then quantity * unit_cost_price else 0 end
  ) stored,
  profit numeric(12,2) generated always as (
    case
      when sale_price_provided and cost_price_provided
      then quantity * (unit_sale_price - unit_cost_price)
      else 0
    end
  ) stored,
  image_url text,
  image_urls text[] not null default '{}',
  note text,
  deleted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists sales_user_created_idx
  on public.sales (user_id, created_at desc);

create index if not exists sales_user_category_idx
  on public.sales (user_id, category_id);

create index if not exists sales_user_product_idx
  on public.sales (user_id, product_id);

alter table public.sales
  drop constraint if exists sales_unit_sale_price_check;

alter table public.sales
  add column if not exists payment_method text not null default 'cash';

alter table public.sales
  add column if not exists money_holder text not null default 'normal';

alter table public.sales
  drop constraint if exists sales_payment_method_check;

alter table public.sales
  add constraint sales_payment_method_check
  check (payment_method in ('cash','card'));

alter table public.sales
  drop constraint if exists sales_money_holder_check;

alter table public.sales
  add constraint sales_money_holder_check
  check (money_holder in ('normal','yusif'));

create table if not exists public.expenses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null default 'Xerc',
  amount numeric(12,2) not null default 0 check (amount >= 0),
  note text,
  money_holder text not null default 'normal' check (money_holder in ('normal','yusif')),
  payment_method text not null default 'cash' check (payment_method in ('cash','card')),
  image_url text,
  image_urls text[] not null default '{}',
  deleted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists expenses_user_created_idx
  on public.expenses (user_id, created_at desc);

create index if not exists expenses_user_amount_idx
  on public.expenses (user_id, amount);

create table if not exists public.notes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  body text not null,
  image_url text,
  image_urls text[] not null default '{}',
  deleted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists notes_user_created_idx
  on public.notes (user_id, created_at desc);

create table if not exists public.app_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  logo_url text,
  favicon_url text,
  report_sections text[] not null default array['sales','expenses'],
  pocket_overrides jsonb not null default '{}',
  daily_profits jsonb not null default '{}',
  show_dashboard_profit boolean not null default true,
  show_cost_profit boolean not null default true,
  show_card_limits boolean not null default false,
  card_accounts text[] not null default array['Əlinin kartı','Yusifin kartı'],
  card_details jsonb not null default '{}',
  card_counter_reset_at timestamptz,
  notebook_images jsonb not null default '[]',
  balance_wallets jsonb not null default '{}',
  quick_expense_buttons jsonb not null default '[{"id":"tea","title":"Çay","amount":2},{"id":"lottery","title":"Latareya","amount":50}]',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.app_settings
  add column if not exists report_sections text[] not null default array['sales','expenses'];

alter table public.app_settings
  alter column report_sections set default array['sales','expenses'];

alter table public.app_settings
  add column if not exists pocket_overrides jsonb not null default '{}';

alter table public.app_settings
  add column if not exists daily_profits jsonb not null default '{}';

alter table public.app_settings
  add column if not exists show_dashboard_profit boolean not null default true;

alter table public.app_settings
  add column if not exists show_cost_profit boolean not null default true;

alter table public.app_settings
  add column if not exists show_card_limits boolean not null default false;

alter table public.app_settings
  add column if not exists card_accounts text[] not null default array['Əlinin kartı','Yusifin kartı'];

alter table public.app_settings
  add column if not exists card_details jsonb not null default '{}';

alter table public.app_settings
  add column if not exists card_counter_reset_at timestamptz;

alter table public.app_settings
  add column if not exists notebook_images jsonb not null default '[]';

alter table public.app_settings
  add column if not exists balance_wallets jsonb not null default '{}';

alter table public.app_settings
  add column if not exists quick_expense_buttons jsonb not null default '[{"id":"tea","title":"Çay","amount":2},{"id":"lottery","title":"Latareya","amount":50}]';

alter table public.products
  add column if not exists deleted_at timestamptz;

alter table public.sales
  add column if not exists deleted_at timestamptz;

alter table public.sales
  add column if not exists card_account text;

alter table public.sales
  add column if not exists is_checked boolean not null default false;

alter table public.expenses
  add column if not exists deleted_at timestamptz;

alter table public.expenses
  add column if not exists money_holder text not null default 'normal';

alter table public.expenses
  add column if not exists payment_method text not null default 'cash';

alter table public.expenses
  drop constraint if exists expenses_money_holder_check;

alter table public.expenses
  add constraint expenses_money_holder_check
  check (money_holder in ('normal','yusif'));

alter table public.expenses
  drop constraint if exists expenses_payment_method_check;

alter table public.expenses
  add constraint expenses_payment_method_check
  check (payment_method in ('cash','card'));

alter table public.notes
  add column if not exists deleted_at timestamptz;

create table if not exists public.activity_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  action text not null check (action in ('insert','update','delete')),
  entity_type text not null,
  entity_id uuid,
  title text not null,
  details jsonb not null default '{}',
  created_at timestamptz not null default now()
);

create index if not exists activity_logs_user_created_idx
  on public.activity_logs (user_id, created_at desc);

create or replace function public.activity_log_title(
  entity_type text,
  action_name text,
  row_data jsonb
)
returns text
language sql
immutable
as $$
  select concat(
    case action_name
      when 'insert' then 'Elave edildi: '
      when 'update' then 'Duzelis edildi: '
      when 'delete' then 'Silindi: '
      else ''
    end,
    case entity_type
      when 'products' then coalesce(row_data->>'name', 'Mehsul')
      when 'sales' then coalesce(row_data->>'product_name', 'Satis')
      when 'expenses' then coalesce(row_data->>'title', 'Xerc')
      when 'notes' then coalesce(row_data->>'body', 'Qeyd')
      when 'categories' then coalesce(row_data->>'name', 'Kateqoriya')
      when 'app_settings' then 'Parametrler'
      else entity_type
    end
  );
$$;

create or replace function public.write_activity_log()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  row_data jsonb;
  row_user_id uuid;
  row_id uuid;
  action_name text;
begin
  action_name := lower(TG_OP);

  if TG_OP = 'DELETE' then
    row_data := to_jsonb(old);
  else
    row_data := to_jsonb(new);
  end if;

  row_user_id := coalesce((row_data->>'user_id')::uuid, (row_data->>'id')::uuid);
  row_id := coalesce((row_data->>'id')::uuid, row_user_id);

  insert into public.activity_logs (
    user_id,
    action,
    entity_type,
    entity_id,
    title,
    details
  )
  values (
    row_user_id,
    action_name,
    TG_TABLE_NAME,
    row_id,
    public.activity_log_title(TG_TABLE_NAME, action_name, row_data),
    row_data
  );

  if TG_OP = 'DELETE' then
    return old;
  end if;

  return new;
end;
$$;

drop trigger if exists categories_set_updated_at on public.categories;
create trigger categories_set_updated_at
before update on public.categories
for each row execute function public.set_updated_at();

drop trigger if exists products_set_updated_at on public.products;
create trigger products_set_updated_at
before update on public.products
for each row execute function public.set_updated_at();

drop trigger if exists sales_set_updated_at on public.sales;
create trigger sales_set_updated_at
before update on public.sales
for each row execute function public.set_updated_at();

drop trigger if exists expenses_set_updated_at on public.expenses;
create trigger expenses_set_updated_at
before update on public.expenses
for each row execute function public.set_updated_at();

drop trigger if exists notes_set_updated_at on public.notes;
create trigger notes_set_updated_at
before update on public.notes
for each row execute function public.set_updated_at();

drop trigger if exists app_settings_set_updated_at on public.app_settings;
create trigger app_settings_set_updated_at
before update on public.app_settings
for each row execute function public.set_updated_at();

drop trigger if exists categories_activity_log on public.categories;
create trigger categories_activity_log
after insert or update or delete on public.categories
for each row execute function public.write_activity_log();

drop trigger if exists products_activity_log on public.products;
create trigger products_activity_log
after insert or update or delete on public.products
for each row execute function public.write_activity_log();

drop trigger if exists sales_activity_log on public.sales;
create trigger sales_activity_log
after insert or update or delete on public.sales
for each row execute function public.write_activity_log();

drop trigger if exists expenses_activity_log on public.expenses;
create trigger expenses_activity_log
after insert or update or delete on public.expenses
for each row execute function public.write_activity_log();

drop trigger if exists notes_activity_log on public.notes;
create trigger notes_activity_log
after insert or update or delete on public.notes
for each row execute function public.write_activity_log();

drop trigger if exists app_settings_activity_log on public.app_settings;
create trigger app_settings_activity_log
after insert or update or delete on public.app_settings
for each row execute function public.write_activity_log();

alter table public.categories enable row level security;
alter table public.products enable row level security;
alter table public.sales enable row level security;
alter table public.expenses enable row level security;
alter table public.notes enable row level security;
alter table public.app_settings enable row level security;
alter table public.activity_logs enable row level security;

drop policy if exists categories_user_access on public.categories;
create policy categories_user_access
on public.categories
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists products_user_access on public.products;
create policy products_user_access
on public.products
for all
using (
  auth.uid() = user_id
  and (
    category_id is null
    or exists (
      select 1 from public.categories c
      where c.id = products.category_id
        and c.user_id = auth.uid()
    )
  )
)
with check (
  auth.uid() = user_id
  and (
    category_id is null
    or exists (
      select 1 from public.categories c
      where c.id = products.category_id
        and c.user_id = auth.uid()
    )
  )
);

drop policy if exists sales_user_access on public.sales;
create policy sales_user_access
on public.sales
for all
using (
  auth.uid() = user_id
  and (
    product_id is null
    or exists (
      select 1 from public.products p
      where p.id = sales.product_id
        and p.user_id = auth.uid()
    )
  )
  and (
    category_id is null
    or exists (
      select 1 from public.categories c
      where c.id = sales.category_id
        and c.user_id = auth.uid()
    )
  )
)
with check (
  auth.uid() = user_id
  and (
    product_id is null
    or exists (
      select 1 from public.products p
      where p.id = sales.product_id
        and p.user_id = auth.uid()
    )
  )
  and (
    category_id is null
    or exists (
      select 1 from public.categories c
      where c.id = sales.category_id
        and c.user_id = auth.uid()
    )
  )
);

drop policy if exists expenses_user_access on public.expenses;
create policy expenses_user_access
on public.expenses
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists notes_user_access on public.notes;
create policy notes_user_access
on public.notes
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists app_settings_user_access on public.app_settings;
create policy app_settings_user_access
on public.app_settings
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists activity_logs_user_access on public.activity_logs;
create policy activity_logs_user_access
on public.activity_logs
for select
using (auth.uid() = user_id);

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'product-images',
  'product-images',
  true,
  12582912,
  array['image/jpeg','image/png','image/webp','image/gif','image/svg+xml']
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists product_images_public_read on storage.objects;
create policy product_images_public_read
on storage.objects
for select
using (bucket_id = 'product-images');

drop policy if exists product_images_user_insert on storage.objects;
create policy product_images_user_insert
on storage.objects
for insert
with check (
  bucket_id = 'product-images'
  and auth.role() = 'authenticated'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists product_images_user_update on storage.objects;
create policy product_images_user_update
on storage.objects
for update
using (
  bucket_id = 'product-images'
  and auth.role() = 'authenticated'
  and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
  bucket_id = 'product-images'
  and auth.role() = 'authenticated'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists product_images_user_delete on storage.objects;
create policy product_images_user_delete
on storage.objects
for delete
using (
  bucket_id = 'product-images'
  and auth.role() = 'authenticated'
  and (storage.foldername(name))[1] = auth.uid()::text
);
