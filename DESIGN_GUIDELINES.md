# 🎨 Cove Manager Design Guidelines

This document outlines the design standards and guidelines for **Cove Manager** to ensure consistency across the application and prevent common design mistakes.

## 📱 Color Palette

### Primary Colors
```xml
<color name="colorPrimary">#4A69FF</color>        <!-- Main brand color -->
<color name="colorPrimaryDark">#3B54CC</color>    <!-- Status bar color -->
<color name="colorBackground">#F5F7FA</color>     <!-- Main background -->
<color name="colorCardBackground">#FFFFFF</color>  <!-- Card backgrounds -->
```

### Text Colors
```xml
<color name="textColorPrimary">#2D3748</color>     <!-- Primary text -->
<color name="textColorSecondary">#718096</color>   <!-- Secondary text -->
<color name="colorIconTint">#8A96A8</color>        <!-- Icon tinting -->
```

### Category Colors (File Types)
```xml
<color name="categoryImages">#77D8A5</color>       <!-- Images - Green -->
<color name="categoryVideos">#62B6FC</color>       <!-- Videos - Blue -->
<color name="categoryAudio">#FFB86C</color>        <!-- Audio - Orange -->
<color name="categoryDocuments">#A992E8</color>    <!-- Documents - Purple -->
<color name="categoryDownloads">#FFD166</color>    <!-- Downloads - Yellow -->
<color name="categoryApks">#FF7B89</color>         <!-- APKs - Pink -->
```

## 📦 MaterialCardView Standards

### Standard Card Configuration
All cards in the app should follow these specifications:

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="2dp"              <!-- ✅ Consistent margin -->
    app:cardBackgroundColor="@color/colorCardBackground"
    app:cardCornerRadius="12dp"              <!-- ✅ Rounded corners -->
    app:cardElevation="1dp">                 <!-- ✅ Subtle elevation -->
    
    <!-- Card content here -->
    
</com.google.android.material.card.MaterialCardView>
```

### Key Rules:
- **Elevation**: Always use `1dp` for subtle, consistent shadows
- **Margin**: Use `2dp` margin for all cards to maintain consistent spacing
- **Corner Radius**: Standard `12dp` for modern rounded appearance
- **Background**: Use `@color/colorCardBackground` for white background

### Common Mistakes to Avoid:
❌ **DON'T** use different elevation values (like 4dp, 8dp)  
❌ **DON'T** use different margin values  
❌ **DON'T** forget to set background color  
❌ **DON'T** use sharp corners (0dp radius)  

## 🔧 Toolbar Configuration

### Standard Toolbar Setup
```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/toolbar"
    android:layout_width="match_parent"
    android:layout_height="?attr/actionBarSize"
    android:background="@color/colorPrimary"
    app:title="Your Title"
    app:titleTextColor="@android:color/white"
    app:theme="@style/ThemeOverlay.MaterialComponents.Dark.ActionBar" />
```

### Critical Settings:
- **Theme**: Always use `ThemeOverlay.MaterialComponents.Dark.ActionBar` for white icons
- **Background**: Use `@color/colorPrimary` for brand consistency
- **Title Color**: Use `@android:color/white` for proper contrast

### AppBarLayout Wrapper:
```xml
<com.google.android.material.appbar.AppBarLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar">
    
    <!-- Toolbar goes here -->
    
</com.google.android.material.appbar.AppBarLayout>
```

## 🎯 Icon Guidelines

### Icon Specifications
- **Size**: Use `32dp` for primary icons in cards
- **Tint**: Apply appropriate category colors for file type icons
- **Margin**: Maintain `12dp` spacing between icon and text

### Icon Usage Examples:
```xml
<ImageView
    android:layout_width="32dp"
    android:layout_height="32dp"
    android:src="@drawable/ic_your_icon"
    android:layout_marginEnd="12dp" />
```

### Available Icons:
- `ic_storage_internal` - Internal storage
- `ic_storage_sd` - SD card storage
- `ic_tool_secure_folder` - Secure folder tool
- `ic_tool_file_cleaner` - File cleaner tool
- `ic_arrow_back` - Navigation back

## 📝 Typography Standards

### Text Sizes and Styles:
```xml
<!-- Section Headers -->
android:textSize="20sp"
android:textStyle="bold"
android:textColor="@color/textColorPrimary"

<!-- Card Titles -->
android:textSize="16sp"
android:textStyle="bold"
android:textColor="@color/textColorPrimary"

<!-- Secondary Information -->
android:textSize="12sp"
android:textColor="@color/textColorSecondary"
```

## 🏗️ Layout Patterns

### Main Content Structure:
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    
    <!-- App Bar -->
    <com.google.android.material.appbar.AppBarLayout>
        <!-- Toolbar -->
    </com.google.android.material.appbar.AppBarLayout>
    
    <!-- Scrollable Content -->
    <androidx.core.widget.NestedScrollView
        app:layout_behavior="@string/appbar_scrolling_view_behavior">
        
        <LinearLayout
            android:orientation="vertical"
            android:padding="16dp">
            
            <!-- Content sections -->
            
        </LinearLayout>
        
    </androidx.core.widget.NestedScrollView>
    
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### Section Header Pattern:
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Section Title"
    android:textColor="@color/textColorPrimary"
    android:textSize="20sp"
    android:textStyle="bold"
    android:layout_marginBottom="12dp" />
```

## 🎨 Interactive Elements

### Clickable Items:
```xml
android:background="?attr/selectableItemBackground"
android:clickable="true"
android:focusable="true"
android:padding="12dp"
```

### Progress Bars:
```xml
<ProgressBar
    style="?android:attr/progressBarStyleHorizontal"
    android:layout_width="0dp"
    android:layout_height="8dp"
    android:progressDrawable="@drawable/rounded_progress" />
```

## 🚫 Common Mistakes to Avoid

### ❌ Elevation Inconsistency
**Wrong:**
```xml
app:cardElevation="4dp"  <!-- Different from standard -->
app:cardElevation="8dp"  <!-- Too high -->
```

**Right:**
```xml
app:cardElevation="1dp"  <!-- Consistent and subtle -->
```

### ❌ Margin Inconsistency
**Wrong:**
```xml
android:layout_margin="8dp"  <!-- Too large -->
android:layout_margin="4dp"  <!-- Different from standard -->
```

**Right:**
```xml
android:layout_margin="2dp"  <!-- Standard spacing -->
```

### ❌ Toolbar Theme Issues
**Wrong:**
```xml
<!-- Missing theme - icons may not be visible -->
<com.google.android.material.appbar.MaterialToolbar
    android:background="@color/colorPrimary" />
```

**Right:**
```xml
<com.google.android.material.appbar.MaterialToolbar
    android:background="@color/colorPrimary"
    app:theme="@style/ThemeOverlay.MaterialComponents.Dark.ActionBar" />
```

### ❌ Text Color Mistakes
**Wrong:**
```xml
android:textColor="#000000"  <!-- Hard-coded black -->
android:textColor="#666666"  <!-- Hard-coded gray -->
```

**Right:**
```xml
android:textColor="@color/textColorPrimary"    <!-- Semantic color -->
android:textColor="@color/textColorSecondary"  <!-- Semantic color -->
```

## ✅ Implementation Checklist

Before submitting any UI changes, ensure:

- [ ] All MaterialCardViews use `cardElevation="1dp"`
- [ ] All cards have `layout_margin="2dp"`
- [ ] All cards use `cardCornerRadius="12dp"`
- [ ] All toolbars include proper theme for icon visibility
- [ ] Text colors use semantic color resources, not hard-coded values
- [ ] Icons are 32dp for primary elements
- [ ] Section headers follow typography standards
- [ ] Interactive elements have proper touch feedback
- [ ] Layout follows the established container patterns

## 🔧 Build and Test

### Before Committing:
1. Run lint check: `./gradlew lintDebug`
2. Build debug APK: `./gradlew assembleDebug`
3. Test on different screen sizes
4. Verify dark mode compatibility (if implemented)

### Automated Checks:
The GitHub Actions workflow will automatically:
- Run lint checks
- Build APK files
- Run unit tests
- Generate reports

## 📞 Questions?

If you encounter design decisions not covered in this guide:
1. Follow the established patterns from existing components
2. Maintain consistency with the color palette
3. Test on multiple screen sizes
4. Consider accessibility implications

---

**Remember**: Consistency is key to a professional, polished user experience. When in doubt, follow the existing patterns and refer to this guide.