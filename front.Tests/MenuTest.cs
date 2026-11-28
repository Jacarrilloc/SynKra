using Bunit;
using Synkra.Front.Components.Layout;

namespace Synkra.Front.Tests;

public class MenuTest : BunitContext
{
    [Fact]
    public void MenuEnlazaLasTresPantallas()
    {
        var cut = Render<NavMenu>();

        var enlaces = cut.FindAll("a");
        Assert.Equal(["Vista general", "Catálogo", "Desplegar"], enlaces.Select(a => a.TextContent.Trim()).ToArray());
        Assert.Equal(["catalogo", "desplegar"], enlaces.Skip(1).Select(a => a.GetAttribute("href")!).ToArray());
        Assert.Contains(enlaces[0].GetAttribute("href"), new[] { "", "/", "http://localhost/" });
    }
}
